package impl.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import impl.shared.ScriptStatus;
import impl.repositories.entities.Script;
import impl.service.exceptions.SyntaxErrorException;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
      ScriptExecutorTest.TestConfig.class
})
public class ScriptExecutorTest {
  private final String FINITE_SCRIPT = "console.log('hello')";
  private final String INFINITE_SCRIPT = "while(true){}";
  private final String SCRIPT_WITH_SYNTAX_ERROR = "#@#$.l()";
  private final String SCRIPT_WITH_EXCEPTION = "throw 'error'";
  private final String FINITE_SCRIPT_RESULT = "hello\n";
  private Script script;

  @Autowired
  ScriptExecutor executor;

  @Configuration
  @EnableScheduling
  @EnableAsync
  public static class TestConfig {
    @Bean(name = "AsyncExecutor")
    public ExecutorService executorService() {
      return Executors.newFixedThreadPool(1);
    }

    @Bean
    public ScriptExecutor executor() {
      return new ScriptExecutor("js");
    }
  }


  @SneakyThrows
  private void await(Script script) {
    executor.awaitTermination(script, 1, TimeUnit.MINUTES);
  }

  private String getStatus(Script exec) {
    return exec.getStatus().get().name();
  }

  private String getOutput(Script exec) {
    return exec.getOutputStream().toString();
  }

  private void executeAsync(String script) {
    AtomicReference<ScriptStatus> status = new AtomicReference<>(ScriptStatus.QUEUE);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    CompletableFuture<Runnable> ctCreation = new CompletableFuture<>();
    CompletableFuture<Void> comp = executor.executeAsync(script, status, ctCreation, outputStream);
    this.script = new Script(script, status, outputStream, comp, ctCreation);
  }

  private void executeBlocking(String script) {
    AtomicReference<ScriptStatus> status = new AtomicReference<>(ScriptStatus.QUEUE);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    CompletableFuture<Runnable> ctCreation = new CompletableFuture<>();
    CompletableFuture<Void> comp = new CompletableFuture<>();
    this.script = new Script(script, status, outputStream, comp, ctCreation);
    executor.execute(script, status, ctCreation, comp, outputStream);
  }

  //      executeScriptAsync

  @Test
  public void shouldPassOnAsyncExec() {
    executeAsync(FINITE_SCRIPT);
    await(script);
    assertEquals(ScriptStatus.DONE.name(), getStatus(script));
    assertEquals(FINITE_SCRIPT_RESULT, getOutput(script));
  }

  @Test
  public void shouldPassOnAsyncExecWithException() {
    executeAsync(SCRIPT_WITH_EXCEPTION);
    assertThatThrownBy(() -> script.getComputation().get())
          .hasCauseExactlyInstanceOf(ExceptResException.class);
  }

  //    executeScript

  @Test
  public void shouldPassOnBlockingExec() {
    executeBlocking(FINITE_SCRIPT);
    assertTrue(script.getComputation().isDone() && ! script.getComputation().isCompletedExceptionally());
    assertEquals(FINITE_SCRIPT_RESULT, getOutput(script));
    assertEquals(ScriptStatus.DONE.name(), getStatus(script));
  }

  @Test
  public void shouldPassOnBlockingExecWithException() {
    executeBlocking(SCRIPT_WITH_EXCEPTION);
    assertTrue(script.getComputation().isCompletedExceptionally());
  }

  //  cancelExec

  @Test
  public void shouldPassOnCancellationExecWithRunningStatus()
        throws ExecutionException, InterruptedException {
    executeAsync(INFINITE_SCRIPT);
    Thread.sleep(100);
    executor.cancelExec(script);
    await(script);
    assertEquals(ScriptStatus.CANCELLED.name(), getStatus(script));
  }

  @Test
  public void shouldPassOnCancellationExecWithFinishedStatus()
        throws ExecutionException, InterruptedException {
    executeAsync(INFINITE_SCRIPT);
    executor.cancelExec(script);
    await(script);
    executor.cancelExec(script);
    assertEquals(ScriptStatus.CANCELLED.name(), getStatus(script));
  }

  @Test
  public void shouldPassWhenMultipleThreadsTryToCancelSimultaneously() throws InterruptedException {
    ExecutorService pool = Executors.newFixedThreadPool(5);
    CountDownLatch successLatch = new CountDownLatch(5);
    CountDownLatch startLatch = new CountDownLatch(6);
    executeAsync(INFINITE_SCRIPT);
    Runnable runnable = () -> {
      try {
        startLatch.countDown();
        startLatch.await();
        executor.cancelExec(script);
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
    await(script);
    assertEquals(ScriptStatus.CANCELLED.name(), getStatus(script));
    assertEquals(0, successLatch.getCount());
  }

  //     awaitTermination

  @Test
  public void shouldPassOnAwaiting()
        throws ExecutionException, InterruptedException, TimeoutException {
    executeAsync(INFINITE_SCRIPT);
    executor.cancelExec(script);
    executor.awaitTermination(script, 1, TimeUnit.MINUTES);
    assertEquals(ScriptStatus.CANCELLED.name(), getStatus(script));
  }

  @Test
  public void shouldFailOnAwaitingWhenTimeout()
        throws ExecutionException, InterruptedException {
    executeAsync(INFINITE_SCRIPT);
    assertThatThrownBy(() -> executor.awaitTermination(script, 1, TimeUnit.SECONDS))
          .isInstanceOf(TimeoutException.class);
    executor.cancelExec(script);
  }

  // checkScript
  @Test
  public void shouldPassOnCheckingScript() {
    assertThatCode(() -> executor.checkScript(FINITE_SCRIPT))
          .doesNotThrowAnyException();
  }

  @Test
  public void shouldFailOnCheckingScriptWithSyntaxError() {
    assertThatThrownBy(() -> executor.checkScript(SCRIPT_WITH_SYNTAX_ERROR))
          .isInstanceOf(SyntaxErrorException.class);
  }

}
