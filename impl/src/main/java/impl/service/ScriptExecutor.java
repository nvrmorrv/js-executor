package impl.service;

import impl.repositories.entities.Execution;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ScriptExecutor {
  private final ExecutorService executorService;
  private final String lang;

  public ScriptExecutor(@Value("${executor.thread-count:}") Integer threadCount,
                        @Value("${executor.lang}") String lang) {
    this.executorService = Executors.newFixedThreadPool(threadCount);
    this.lang = lang;
  }

  public Execution executeAsync(String script) {
    throwIfPoolIsShutdown();
    AtomicReference<ExecStatus> status = new AtomicReference<>(ExecStatus.QUEUE);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();   //  has sync methods and is to be closed by GC
    CompletableFuture<Runnable> ctCreation = new CompletableFuture<>();
    CompletableFuture<Void> comp = CompletableFuture.runAsync(
                () -> runScript(script, outputStream, status, ctCreation),
          executorService);
    return new Execution(status, outputStream, comp, ctCreation);
  }

  public Execution execute(String script, long execTimeout, TimeUnit unit)
        throws ExecutionException, InterruptedException, TimeoutException {
    throwIfPoolIsShutdown();
    Execution exec = executeAsync(script);
    try {
      exec.getComputation().get(execTimeout, unit);
      return exec;
    } catch (TimeoutException ex) {
      cancelExec(exec);
      throw ex;
    }
  }

  public void cancelExec(Execution exec) throws ExecutionException, InterruptedException {
    cancel(exec.getCtCreation());
  }

  public void awaitTermination(Execution exec, long execTimeout, TimeUnit unit)
        throws ExecutionException, InterruptedException, TimeoutException {
    exec.getComputation().get(execTimeout, unit);
  }

  public void shutdown() {
    executorService.shutdown();
  }

  public boolean shutdownAndAwaitAll(long timeout, TimeUnit timeUnit)
        throws InterruptedException {
    if(!executorService.isShutdown()) {
      shutdown();
    }
    return executorService.awaitTermination(timeout, timeUnit);
  }

  private void runScript(String script,
                         OutputStream stream,
                         AtomicReference<ExecStatus> status,
                         CompletableFuture<Runnable> ctCreation) {
    status.set(ExecStatus.RUNNING);
    try (Context context = createContext(stream)) {
      checkCancelAndComplete(ctCreation, context);
      context.eval(lang, script);
      status.set(ExecStatus.DONE);
    } catch (PolyglotException ex) {
      if(ex.getMessage().contains("SyntaxError")) {
        status.set(ExecStatus.DONE_WITH_SYNTAX_ERROR);
      } else if(ex.getMessage().contains("Execution got cancelled")) {
        status.set(ExecStatus.CANCELLED);
      } else {
        status.set(ExecStatus.DONE_WITH_EXCEPTION);
      }
    } catch (IllegalStateException ex) {
      status.set(ExecStatus.CANCELLED);
    }
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

  private Context createContext(OutputStream outputStream) {
    return Context.newBuilder(lang)
          .out(outputStream)
          .build();
  }

  private void throwIfPoolIsShutdown() {
    if (executorService.isShutdown()) {
      throw new IllegalStateException("Script executor is already shutdown");
    }
  }
}
