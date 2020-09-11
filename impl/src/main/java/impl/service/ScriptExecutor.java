package impl.service;

import impl.repositories.entities.Execution;
import impl.service.exceptions.ExceptResException;
import impl.service.exceptions.SyntaxErrorException;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.catalina.connector.ClientAbortException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ScriptExecutor {
  private final String lang;

  public ScriptExecutor(@Value("${executor.lang}") String lang) {
    this.lang = lang;
  }

  public static String getOutput(OutputStream stream) {
    return stream.toString();
  }

  public void execute(String script,
                      AtomicReference<ExecStatus> status,
                      CompletableFuture<Runnable> ctCreation,
                      CompletableFuture<Void> computation,
                      OutputStream outputStream) {
    try {
      executeScript(script, status, ctCreation, outputStream);
      computation.complete(null);
    } catch (ExceptResException ex) {
      computation.completeExceptionally(ex);
    }
  }

  @Async
  public CompletableFuture<Void> executeAsync(String script,
               AtomicReference<ExecStatus> status,
               CompletableFuture<Runnable> ctCreation,
               ByteArrayOutputStream outputStream) {
    executeScript(script, status, ctCreation, outputStream);
    return CompletableFuture.completedFuture(null);
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


  private void executeScript(String script,
                        AtomicReference<ExecStatus> status,
                        CompletableFuture<Runnable> ctCreation,
                        OutputStream outputStream) {
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

  private Context createContext() {
    return Context.newBuilder(lang).build();
  }
}
