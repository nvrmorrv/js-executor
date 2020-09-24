package impl.service;

import impl.aspects.annotations.Running;
import impl.repositories.entities.Script;
import impl.service.dto.ScriptInfo;
import impl.shared.ExecStatus;
import impl.service.exceptions.SyntaxErrorException;
import io.micrometer.core.annotation.Timed;

import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.Getter;
import lombok.SneakyThrows;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ScriptImpl implements Script {
  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final CompletableFuture<Runnable> ctCreation = new CompletableFuture<>();
  private final String lang;
  @Getter
  private final String id;
  @Getter
  private final byte[] source;
  private final TimeZone timeZone;
  private final ZonedDateTime createTime;
  private ZonedDateTime startTime;
  private ZonedDateTime finishTime;
  private ByteArrayOutputStream outputStream;
  @Getter
  private ExecStatus status = ExecStatus.QUEUE;
  private String exMessage;
  private List<String> stackTrace;

  public ScriptImpl(String lang, String id, byte[] source, TimeZone timeZone) {
    this.lang = lang;
    this.source = source;
    checkScript();
    this.id = id;
    this.timeZone = timeZone;
    this.createTime = ZonedDateTime.now(timeZone.toZoneId());
  }

  @Override
  public void executeScript() {
    this.outputStream = new ByteArrayOutputStream();
    execute();
  }

  @Override
  public void executeScript(OutputStream stream) {
    this.outputStream = new OutputStreamWrapper(stream);
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

  @Override
  public byte[] getOutput() {
    return (outputStream != null) ? outputStream.toByteArray() : new byte[0];
  }

  @Override
  public ScriptInfo getScriptInfo() {
    lock.readLock().lock();
    ScriptInfo info = new ScriptInfo(
          id,
          status,
          createTime,
          Optional.ofNullable(startTime),
          Optional.ofNullable(finishTime),
          Optional.ofNullable(exMessage),
          Optional.ofNullable(stackTrace));
    lock.readLock().unlock();
    return info;
  }


  @Running
  @Timed(value = "running_time")
  private void execute() {
    setStart();
    try (Context context = createContext()) {
      checkCancelAndComplete(context);
      context.eval(lang, new String(source));
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

  private void checkScript() {
    try(Context context = Context.newBuilder(lang).build()) {
      context.parse(lang, new String(source));
    } catch (PolyglotException ex) {
      throw new SyntaxErrorException(ex.getMessage(),
            ex.getSourceLocation().getCharacters().toString());
    }
  }

  private Context createContext() {
    return Context.newBuilder(lang)
          .out(outputStream)
          .err(outputStream)
          .build();
  }

  private List<String> getGuestStackTrace(Iterable<PolyglotException.StackFrame> frames) {
    return StreamSupport.stream(frames.spliterator(), false)
          .filter(PolyglotException.StackFrame::isGuestFrame)
          .map(PolyglotException.StackFrame::toString)
          .collect(Collectors.toList());
  }
}
