package impl.service;

import impl.aspects.annotations.Running;
import impl.repositories.entities.ExecStatus;
import impl.repositories.entities.Script;
import impl.service.exceptions.SyntaxErrorException;
import io.micrometer.core.annotation.Timed;
import lombok.Getter;
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

public class JsScript implements Script {
  private final String LANG = "js";
  @Getter private final String id;
  @Getter private final String script;
  private final ByteArrayOutputStream outputStream;
  private final ByteArrayOutputStream errStream = new ByteArrayOutputStream();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final Context context = createContext();
  @Getter private final Instant scheduledTime = Instant.now();
  @Getter private Instant startTime;
  @Getter private Instant endTime;
  @Getter private ExecStatus status = ExecStatus.QUEUE;
  private String exMessage;
  private List<String> stackTrace;

  public JsScript(String id, String script) {
    checkScript(script);
    this.id = id;
    this.script = script;
    this.outputStream = new ByteArrayOutputStream();
  }

  public JsScript(String id, String script, OutputStream stream) {
    checkScript(script);
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
    start();
    try (Context context = this.context) {
      context.eval(LANG, script);
      end(ExecStatus.DONE);
    } catch (PolyglotException ex) {
      if (ex.isCancelled()) {
        end(ExecStatus.CANCELLED);
      } else {
   //     endWithException(ex.getMessage(), ex.getStackTrace());
      }
    } catch (IllegalStateException ex) {
      end(ExecStatus.CANCELLED);
    }
  }

  @Async
  @Override
  public void executeAsync() {
    execute();
  }

  @Override
  public void cancel() {
    context.close(true);
  }

  private void start() {
    lock.writeLock().lock();
    status = ExecStatus.RUNNING;
    startTime = Instant.now();
    lock.writeLock().unlock();
  }

  private void end(ExecStatus eStatus) {
    lock.writeLock().lock();
    status = eStatus;
    endTime = Instant.now();
    lock.writeLock().unlock();
  }

  private void endWithException(String message, List<String> sTrace) {
    lock.writeLock().lock();
    status = ExecStatus.DONE_WITH_EXCEPTION;
    endTime = Instant.now();
    exMessage = message;
    stackTrace = sTrace;
    lock.writeLock().unlock();
  }

  private void checkScript(String script) {
    try(Context context = createCheckContext()) {
      context.parse(LANG, script);
    } catch (PolyglotException ex) {
      throw new SyntaxErrorException(ex.getMessage(),
            ex.getSourceLocation().getCharacters().toString());
    }
  }

  private Context createContext() {
    return  Context.newBuilder(LANG)
          .out(outputStream)
          .err(errStream)
          .build();
  }

  private Context createCheckContext() {
    return Context.newBuilder(LANG).build();
  }
}
