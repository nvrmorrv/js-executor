package impl.service;

import impl.repositories.entities.Execution;
import impl.service.exceptions.ExceptResException;
import impl.service.exceptions.ExecTimeOutException;
import impl.service.exceptions.SyntaxErrorException;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScriptExecutor {
  private final List<BlockingExec> blockingExecs = new CopyOnWriteArrayList<>();
  private final String lang;

  public ScriptExecutor(@Value("${executor.lang}") String lang) {
    this.lang = lang;
  }

  public static String getOutput(ByteArrayOutputStream stream) {
    return new String(stream.toByteArray());
  }

  public String execute(String script, long timeout, TimeUnit timeUnit) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try (Context context = createContext(outputStream)) {
      addNewBlockingExec(context, timeout, timeUnit);
      context.eval(lang, script);
      return getOutput(outputStream);
    } catch (PolyglotException e) {
      if (e.isSyntaxError()) {
        throw new SyntaxErrorException(
              e.getMessage(), e.getSourceLocation().getCharacters().toString());
      } else if (e.isCancelled()) {
        throw new ExecTimeOutException(
              timeout, timeUnit, getOutput(outputStream));
      } else {
        throw new ExceptResException(
              e.getMessage(), getOutput(outputStream));
      }
    } catch (IllegalStateException e) {
      throw new ExecTimeOutException(
            timeout, timeUnit, getOutput(outputStream));
    }
  }

  @Scheduled(fixedRate = 100)
  public void checkTimeoutAndInterrupt() {
    blockingExecs.stream()
          .filter(exec -> exec.getExecDuration()
                .minus(Duration.between(exec.startTime, Instant.now()))
                .isNegative())
          .forEach(exec ->  {
            exec.getCancellation().run();
            blockingExecs.remove(exec);
          });
  }

  public void awaitTermination(Execution exec, long execTimeout, TimeUnit unit)
        throws ExecutionException, InterruptedException, TimeoutException {
    exec.getComputation().get(execTimeout, unit);
  }

  public void checkScript(String script) {
    try(Context context = createContext()) {
      context.parse(lang, script);
    } catch (PolyglotException ex) {
      throw new SyntaxErrorException(ex.getMessage(),
            ex.getSourceLocation().getCharacters().toString());
    }
  }

  public void cancelExec(Execution exec)
        throws ExecutionException, InterruptedException {
    cancel(exec.getCtCreation());
  }


  @Async("AsyncExecutor")
  public CompletableFuture<Void> executeAsync(String script,
                                              AtomicReference<ExecStatus> status,
                                              CompletableFuture<Runnable> ctCreation,
                                              ByteArrayOutputStream outputStream) {
    status.set(ExecStatus.RUNNING);
    try (Context context = createContext(outputStream)) {
      checkCancelAndComplete(ctCreation, context);
      context.eval(lang, script);
      status.set(ExecStatus.DONE);
    } catch (PolyglotException ex) {
      if (ex.isCancelled()) {
        status.set(ExecStatus.CANCELLED);
      } else {
        throw new ExceptResException(
              ex.getMessage(), getOutput(outputStream));
      }
    } catch (IllegalStateException ex) {
      status.set(ExecStatus.CANCELLED);
    }
    return CompletableFuture.completedFuture(null);
  }

  private void checkCancelAndComplete(CompletableFuture<Runnable> ctCreation,
                                      Context context) {
    synchronized (ctCreation) {
      if(ctCreation.isCancelled()){
        throw new IllegalStateException();
      } else {
        ctCreation.complete(() -> context.close(true));
      }
    }
  }

  private void cancel(CompletableFuture<Runnable> ctCreation)
        throws ExecutionException, InterruptedException {
    synchronized (ctCreation) {
      if(!ctCreation.isCancelled()) {
        if (ctCreation.isDone()) {
          ctCreation.get().run();
        }
        ctCreation.cancel(true);
      }
    }
  }

  private void addNewBlockingExec(Context context, long timeout, TimeUnit timeUnit) {
    blockingExecs.add(new BlockingExec(
          Instant.now(),
          Duration.of(timeout, timeUnit.toChronoUnit()),
          () -> context.close(true)
    ));
  }

  private Context createContext(OutputStream outputStream) {
    return Context.newBuilder(lang)
          .out(outputStream)
          .build();
  }

  private Context createContext() {
    return Context.newBuilder(lang).build();
  }

  @AllArgsConstructor
  @Getter
  public static class BlockingExec {
    private final Instant startTime;
    private final Duration execDuration;
    private final Runnable cancellation;
  }
}
