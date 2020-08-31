package impl.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import impl.repositories.entities.Execution;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ScriptExecutorTest {
  private ScriptExecutor executor;
  private final String FINITE_SCRIPT = "console.log('hello')";
  private final String INFINITE_SCRIPT = "while(true){}";
  private final String SCRIPT_WITH_SYNTAX_ERROR = "#@#$.l()";
  private final String SCRIPT_WITH_EXCEPTION = "throw 'error'";
  private final String FINITE_SCRIPT_RESULT = "hello\n";

  @BeforeEach
  public void setup() {
    executor = new ScriptExecutor(1, "js");
  }

  @SneakyThrows
  public void await(Execution execution) {
    executor.awaitTermination(execution, 1, TimeUnit.MINUTES);
  }

  public String getStatus(Execution exec) {
    return exec.getStatus().get().name();
  }

  public String getOutput(Execution exec) {
    return exec.getOutputStream().toString();
  }

  //      executeScriptAsync

  @Test
  public void shouldPassOnAsyncExec() {
    Execution exec = executor.executeAsync(FINITE_SCRIPT);
    await(exec);
    assertEquals(ExecStatus.DONE.name(), getStatus(exec));
    assertEquals(FINITE_SCRIPT_RESULT, getOutput(exec));
  }

  @Test
  public void shouldPassOnAsyncExecWithSyntaxError() {
    Execution exec = executor.executeAsync(SCRIPT_WITH_SYNTAX_ERROR);
    await(exec);
    assertEquals(ExecStatus.DONE_WITH_SYNTAX_ERROR.name(), getStatus(exec));
    assertEquals("", getOutput(exec));
  }

  @Test
  public void shouldPassOnAsyncExecWithException() {
    Execution exec = executor.executeAsync(SCRIPT_WITH_EXCEPTION);
    await(exec);
    assertEquals(ExecStatus.DONE_WITH_EXCEPTION.name(), getStatus(exec));
    assertEquals("", getOutput(exec));
  }

  @Test
  public void shouldFailOnAsyncExecWhenServiceWasClosed() {
    executor.shutdown();
    assertThatThrownBy(() -> executor.executeAsync(FINITE_SCRIPT))
          .isInstanceOf(IllegalStateException.class);
  }

  //    executeScript

  @Test
  public void shouldPassOnBlockingExec()
        throws ExecutionException, InterruptedException, TimeoutException {
    Execution exec = executor.execute(FINITE_SCRIPT, 30, TimeUnit.SECONDS);
    assertEquals(ExecStatus.DONE.name(), getStatus(exec));
    assertEquals(FINITE_SCRIPT_RESULT, getOutput(exec));
  }

  @Test
  public void shouldPassOnBlockingExecWithSyntaxError()
        throws ExecutionException, InterruptedException, TimeoutException {
    Execution exec = executor.execute(SCRIPT_WITH_SYNTAX_ERROR, 30, TimeUnit.SECONDS);
    assertEquals(ExecStatus.DONE_WITH_SYNTAX_ERROR.name(), getStatus(exec));
    assertEquals("", getOutput(exec));
  }

  @Test
  public void shouldPassOnBlockingExecWithException()
        throws ExecutionException, InterruptedException, TimeoutException {
    Execution exec = executor.execute(SCRIPT_WITH_EXCEPTION, 30, TimeUnit.SECONDS);
    assertEquals(ExecStatus.DONE_WITH_EXCEPTION.name(), getStatus(exec));
    assertEquals("", getOutput(exec));
  }

  @Test
  public void shouldFailOnBlockingExecWhenServiceWasClosed() {
    executor.shutdown();
    assertThatThrownBy(() ->
          executor.execute(FINITE_SCRIPT, 30, TimeUnit.SECONDS))
          .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void shouldFailOnBlockingExecWhenTimeout() {
    assertThatThrownBy(() ->
          executor.execute(INFINITE_SCRIPT, 1, TimeUnit.SECONDS))
          .isInstanceOf(TimeoutException.class);
  }

  //  cancelExec

  @Test
  public void shouldPassOnCancellationExecWithRunningStatus()
        throws ExecutionException, InterruptedException {
    Execution exec = executor.executeAsync(INFINITE_SCRIPT);
    Thread.sleep(100);
    executor.cancelExec(exec);
    await(exec);
    assertEquals(ExecStatus.CANCELLED.name(), getStatus(exec));
  }

  @Test
  public void shouldPassOnCancellationExecWithFinishedStatus()
        throws ExecutionException, InterruptedException {
    Execution exec = executor.executeAsync(INFINITE_SCRIPT);
    executor.cancelExec(exec);
    await(exec);
    executor.cancelExec(exec);
    assertEquals(ExecStatus.CANCELLED.name(), getStatus(exec));
  }

  @Test
  public void shouldPassWhenMultipleThreadsTryToCancelSimultaneously() throws InterruptedException {
    ExecutorService pool = Executors.newFixedThreadPool(5);
    CountDownLatch successLatch = new CountDownLatch(5);
    CountDownLatch startLatch = new CountDownLatch(6);
    Execution exec = executor.executeAsync(INFINITE_SCRIPT);
    Runnable runnable = () -> {
      try {
        startLatch.countDown();
        startLatch.await();
        executor.cancelExec(exec);
        successLatch.countDown();
      } catch (Exception e) {
        e.printStackTrace();
      }
    };
    pool.execute(runnable);
    pool.execute(runnable);
    pool.execute(runnable);
    pool.execute(runnable);
    pool.execute(runnable);
    startLatch.countDown();
    pool.shutdown();
    pool.awaitTermination(30, TimeUnit.SECONDS);
    await(exec);
    assertEquals(ExecStatus.CANCELLED.name(), getStatus(exec));
    assertEquals(0, successLatch.getCount());
  }

  //     awaitTermination

  @Test
  public void shouldPassOnAwaiting()
        throws ExecutionException, InterruptedException, TimeoutException {
    Execution exec = executor.executeAsync(INFINITE_SCRIPT);
    executor.cancelExec(exec);
    executor.awaitTermination(exec, 1, TimeUnit.MINUTES);
    assertEquals(ExecStatus.CANCELLED.name(), getStatus(exec));
  }

  @Test
  public void shouldFailOnAwaitingWhenTimeout()
        throws ExecutionException, InterruptedException {
    Execution exec = executor.executeAsync(INFINITE_SCRIPT);
    assertThatThrownBy(() -> executor.awaitTermination(exec, 1, TimeUnit.SECONDS))
          .isInstanceOf(TimeoutException.class);
    executor.cancelExec(exec);
  }

  //    shutdown

  @Test
  public void shouldFailOnShutdown() {
    executor.shutdown();
    assertThatThrownBy(() -> executor.executeAsync(FINITE_SCRIPT))
          .isInstanceOf(IllegalStateException.class);
  }

  //    shutdownAndAwaitAll

  @Test
  public void shouldPassOnShutdownAndAwaitingAll() throws InterruptedException {
    String longScript = "val = 0; while(val < 1000000){val++;}";
    Execution exec  = executor.executeAsync(longScript);
    executor.shutdownAndAwaitAll(30, TimeUnit.SECONDS);
    assertEquals(ExecStatus.DONE.name(), getStatus(exec));
  }

  @Test
  public void shouldPassOnAwaitingAllWithTimeout()
        throws InterruptedException, ExecutionException {
    Execution exec  = executor.executeAsync(INFINITE_SCRIPT);
    assertFalse(executor.shutdownAndAwaitAll(1, TimeUnit.SECONDS));
    executor.cancelExec(exec);
  }
}
