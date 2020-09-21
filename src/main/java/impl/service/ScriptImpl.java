package impl.service;

import impl.aspects.annotations.Running;
import impl.repositories.entities.ExecStatus;
import impl.repositories.entities.Script;
import impl.service.exceptions.SyntaxErrorException;
import io.micrometer.core.annotation.Timed;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.Getter;
import lombok.SneakyThrows;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.springframework.scheduling.annotation.Async;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ScriptImpl implements Script {
  private final String lang;
  @Getter private final String id;
  @Getter private final String script;
  private final ByteArrayOutputStream outputStream;
  private final ByteArrayOutputStream errStream = new ByteArrayOutputStream();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  @Getter private final Instant scheduledTime = Instant.now();
  @Getter private Instant startTime;
  @Getter private Instant endTime;
  @Getter private ExecStatus status = ExecStatus.QUEUE;
  private String exMessage;
  private List<String> stackTrace;
  private final CompletableFuture<Runnable> ctCreation = new CompletableFuture<>();

  public ScriptImpl(String lang, String id, String script) {
    checkScript(script);
    this.lang = lang;
    this.id = id;
    this.script = script;
    this.outputStream = new ByteArrayOutputStream();
  }

  public ScriptImpl(String lang, String id, String script, OutputStream stream) {
    checkScript(script);
    this.lang = lang;
    this.id = id;
    this.script = script;
    this.outputStream = new OutputStreamWrapper(stream);
  }

  @Override
  public byte[] getOutput() {
    return outputStream.toByteArray();
  }

  @Override
  public byte[] getErrOutput() {
    return errStream.toByteArray();
  }

  @Override
  public Optional<String> getExMessage() {
    return Optional.ofNullable(exMessage);
  }

  @Override
  public Optional<List<String>> getStackTrace() {
    return Optional.ofNullable(stackTrace);
  }

  @Override
  public Lock getReadLock() {
    return lock.readLock();
  }

  @Running
  @Timed(value = "running_time")
  @Override
  public void execute() {
    setStart();
    try (Context context = createContext()) {
      checkCancelAndComplete(context);
      context.eval(lang, script);
      setEnd(ExecStatus.DONE);
    } catch (PolyglotException ex) {
      if (ex.isCancelled()) {
        setEnd(ExecStatus.CANCELLED);
      } else {
        setEndWithException(ex.getMessage(),
              getGuestStackTrace(ex.getPolyglotStackTrace()));
      }
    } catch (IllegalStateException ex) {
      setEnd(ExecStatus.CANCELLED);
    }
  }

  @Async
  @Override
  public void executeAsync() {
    execute();
  }

  @SneakyThrows
  public void cancel() {
    synchronized (ctCreation) {
      if(!ctCreation.isCancelled()) {
        if (ctCreation.isDone()) {
          ctCreation.get().run();
        }
        ctCreation.cancel(true);
      }
    }
  }

  private void checkCancelAndComplete(Context context) {
    synchronized (ctCreation) {
      if(ctCreation.isCancelled()){
        throw new IllegalStateException();
      } else {
        ctCreation.complete(() -> context.close(true));
      }
    }
  }

  private void setStart() {
    lock.writeLock().lock();
    status = ExecStatus.RUNNING;
    startTime = Instant.now();
    lock.writeLock().unlock();
  }

  private void setEnd(ExecStatus eStatus) {
    lock.writeLock().lock();
    status = eStatus;
    endTime = Instant.now();
    lock.writeLock().unlock();
  }

  private void setEndWithException(String message, List<String> sTrace) {
    lock.writeLock().lock();
    status = ExecStatus.DONE_WITH_EXCEPTION;
    endTime = Instant.now();
    exMessage = message;
    stackTrace = sTrace;
    lock.writeLock().unlock();
  }

  private void checkScript(String script) {
    try(Context context = Context.newBuilder(lang).build()) {
      context.parse(lang, script);
    } catch (PolyglotException ex) {
      throw new SyntaxErrorException(ex.getMessage(),
            ex.getSourceLocation().getCharacters().toString());
    }
  }

  private Context createContext() {
    return  Context.newBuilder(lang)
          .out(outputStream)
          .err(errStream)
          .build();
  }

  private List<String> getGuestStackTrace(Iterable<PolyglotException.StackFrame> frames) {
    return StreamSupport.stream(frames.spliterator(), false)
          .filter(PolyglotException.StackFrame::isGuestFrame)
          .map(PolyglotException.StackFrame::toString)
          .collect(Collectors.toList());
  }
}
