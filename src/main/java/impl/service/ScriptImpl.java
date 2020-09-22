package impl.service;

import impl.aspects.annotations.Running;
import impl.repositories.entities.ExecStatus;
import impl.repositories.entities.Script;
import impl.service.exceptions.SyntaxErrorException;
import io.micrometer.core.annotation.Timed;

import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.Getter;
import lombok.SneakyThrows;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.springframework.scheduling.annotation.Async;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ScriptImpl implements Script {
  private final String lang;
  @Getter
  private final String id;
  @Getter
  private final byte[] script;
  private final TimeZone timeZone;
  private final ZonedDateTime scheduledTime;
  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-uuuu;HH:mm:ss");
  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final CompletableFuture<Runnable> ctCreation = new CompletableFuture<>();
  private ZonedDateTime startTime;
  private ZonedDateTime finishTime;
  private ByteArrayOutputStream outputStream;
  private ByteArrayOutputStream errStream;
  @Getter
  private ExecStatus status = ExecStatus.QUEUE;
  private String exMessage;
  private List<String> stackTrace;

  public ScriptImpl(String lang, String id, byte[] script, TimeZone timeZone) {
    checkScript(new String(script));
    this.lang = lang;
    this.id = id;
    this.script = script;
    this.timeZone = timeZone;
    this.scheduledTime = ZonedDateTime.now(timeZone.toZoneId());
  }

  @Override
  public String getScheduledTime() {
    return scheduledTime.format(formatter);
  }

  @Override
  public String getStartTime() {
    return (startTime != null ) ? startTime.format(formatter) : "";
  }

  @Override
  public String getFinishTime() {
    return (finishTime != null ) ? finishTime.format(formatter) : "";
  }

  @Override
  public byte[] getOutput() {
    return (outputStream != null) ? outputStream.toByteArray() : new byte[0];
  }

  @Override
  public byte[] getErrOutput() {
    return (errStream != null) ? errStream.toByteArray() : new byte[0];
  }

  @Override
  public String getExMessage() {
    return (exMessage != null) ? exMessage : "";
  }

  @Override
  public List<String> getStackTrace() {
    return (stackTrace != null) ? stackTrace : Collections.emptyList();
  }

  @Override
  public Lock getReadLock() {
    return lock.readLock();
  }

  @Running
  @Timed(value = "running_time")
  private void execute() {
    setStart();
    try (Context context = createContext()) {
      checkCancelAndComplete(context);
      context.eval(lang, new String(script));
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
    this.outputStream = new ByteArrayOutputStream();
    this.errStream = new ByteArrayOutputStream();
    execute();
  }

  @Override
  public void execute(OutputStream stream) {
    this.outputStream = new OutputStreamWrapper(outputStream);
    this.errStream = new OutputStreamWrapper(errStream);
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
    startTime = ZonedDateTime.now(timeZone.toZoneId());
    lock.writeLock().unlock();
  }

  private void setEnd(ExecStatus eStatus) {
    lock.writeLock().lock();
    status = eStatus;
    finishTime = ZonedDateTime.now(timeZone.toZoneId());
    lock.writeLock().unlock();
  }

  private void setEndWithException(String message, List<String> sTrace) {
    lock.writeLock().lock();
    status = ExecStatus.DONE_WITH_EXCEPTION;
    finishTime = ZonedDateTime.now(timeZone.toZoneId());
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
    return Context.newBuilder(lang)
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
