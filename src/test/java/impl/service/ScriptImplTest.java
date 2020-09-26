package impl.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import impl.shared.ScriptStatus;
import impl.repositories.entities.Script;
import impl.service.exceptions.SyntaxErrorException;
import java.io.ByteArrayOutputStream;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ScriptImplTest {
  private final byte[] FINITE_SCRIPT = "console.error('hello');console.log('hello')".getBytes();
  private final byte[] INFINITE_SCRIPT = "while(true){}".getBytes();
  private final byte[] SCRIPT_WITH_SYNTAX_ERROR = "#@#$.l()".getBytes();
  private final byte[] SCRIPT_WITH_EXCEPTION = "console.error('hello'); throw 'error'".getBytes();
  private final byte[] FINITE_SCRIPT_RESULT = "hello\nhello\n".getBytes();
  private final byte[] SCRIPT_WITH_EXCEPTION_RESULT = "hello\n".getBytes();
  private final String SCRIPT_ID = "id";
  private Script script;
  private ExecutorService pool;

  @BeforeEach
  public void setup() {
    this.pool = Executors.newSingleThreadExecutor();
  }

  @AfterEach
  public void closePool() {
    this.pool.shutdown();
  }

  @SneakyThrows
  private void await() {
    pool.awaitTermination(3, TimeUnit.SECONDS);
  }

  private void createScript(byte[] source) {
    this.script = new ScriptImpl("js", SCRIPT_ID, source, TimeZone.getDefault());
  }

  private void executeAsync() {
    pool.execute(() -> script.executeScript());
  }

  @Test
  public void shouldPassOnCreating() {
    createScript(FINITE_SCRIPT);
    assertEquals(SCRIPT_ID, script.getId());
    assertEquals(ScriptStatus.QUEUE, script.getStatus());
    assertArrayEquals(FINITE_SCRIPT, script.getSource());
    assertArrayEquals(new byte[0], script.getOutput());
    assertEquals(SCRIPT_ID, script.getScriptInfo().getId());
    assertEquals(ScriptStatus.QUEUE, script.getScriptInfo().getStatus());
    assertNotNull(script.getScriptInfo().getCreateTime());
    assertTrue(script.getScriptInfo().getStartTime().isEmpty());
    assertTrue(script.getScriptInfo().getFinishTime().isEmpty());
    assertTrue(script.getScriptInfo().getMessage().isEmpty());
    assertTrue(script.getScriptInfo().getStackTrace().isEmpty());
  }

  @Test
  public void shouldFailOnSyntaxError() {
    assertThatThrownBy(() -> createScript(SCRIPT_WITH_SYNTAX_ERROR))
          .isInstanceOf(SyntaxErrorException.class);
  }

  //      execute

  @Test
  public void shouldPassOnExec() {
    createScript(FINITE_SCRIPT);
    script.executeScript();
    assertEquals(SCRIPT_ID, script.getId());
    assertEquals(ScriptStatus.DONE, script.getStatus());
    assertArrayEquals(FINITE_SCRIPT, script.getSource());
    assertArrayEquals(FINITE_SCRIPT_RESULT, script.getOutput());
    assertEquals(SCRIPT_ID, script.getScriptInfo().getId());
    assertEquals(ScriptStatus.DONE, script.getScriptInfo().getStatus());
    assertNotNull(script.getScriptInfo().getCreateTime());
    assertTrue(script.getScriptInfo().getStartTime().isPresent());
    assertTrue(script.getScriptInfo().getFinishTime().isPresent());
    assertTrue(script.getScriptInfo().getMessage().isEmpty());
    assertTrue(script.getScriptInfo().getStackTrace().isEmpty());
  }

  @Test
  public void shouldPassOnExecScriptWithException() {
    createScript(SCRIPT_WITH_EXCEPTION);
    script.executeScript();
    assertEquals(SCRIPT_ID, script.getId());
    assertEquals(ScriptStatus.DONE_WITH_EXCEPTION, script.getStatus());
    assertArrayEquals(SCRIPT_WITH_EXCEPTION, script.getSource());
    assertArrayEquals(SCRIPT_WITH_EXCEPTION_RESULT, script.getOutput());
    assertEquals(SCRIPT_ID, script.getScriptInfo().getId());
    assertEquals(ScriptStatus.DONE_WITH_EXCEPTION, script.getScriptInfo().getStatus());
    assertNotNull(script.getScriptInfo().getCreateTime());
    assertTrue(script.getScriptInfo().getStartTime().isPresent());
    assertTrue(script.getScriptInfo().getFinishTime().isPresent());
    assertEquals("error", script.getScriptInfo().getMessage().get());
    assertTrue(script.getScriptInfo().getStackTrace().isPresent());
  }

  @Test
  public void shouldPassOnAsyncExec() {
    createScript(FINITE_SCRIPT);
    executeAsync();
    await();
    assertEquals(SCRIPT_ID, script.getId());
    assertEquals(ScriptStatus.DONE, script.getStatus());
    assertArrayEquals(FINITE_SCRIPT, script.getSource());
    assertArrayEquals(FINITE_SCRIPT_RESULT, script.getOutput());
    assertEquals(SCRIPT_ID, script.getScriptInfo().getId());
    assertEquals(ScriptStatus.DONE, script.getScriptInfo().getStatus());
    assertNotNull(script.getScriptInfo().getCreateTime());
    assertTrue(script.getScriptInfo().getStartTime().isPresent());
    assertTrue(script.getScriptInfo().getFinishTime().isPresent());
    assertTrue(script.getScriptInfo().getMessage().isEmpty());
    assertTrue(script.getScriptInfo().getStackTrace().isEmpty());
  }

  @Test
  public void shouldPassOnExecWithStream() {
    createScript(FINITE_SCRIPT);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    script.executeScript(stream);
    assertEquals(SCRIPT_ID, script.getId());
    assertEquals(ScriptStatus.DONE, script.getStatus());
    assertArrayEquals(FINITE_SCRIPT, script.getSource());
    assertArrayEquals(FINITE_SCRIPT_RESULT, script.getOutput());
    assertArrayEquals(FINITE_SCRIPT_RESULT, stream.toByteArray());
    assertEquals(SCRIPT_ID, script.getScriptInfo().getId());
    assertEquals(ScriptStatus.DONE, script.getScriptInfo().getStatus());
    assertNotNull(script.getScriptInfo().getCreateTime());
    assertTrue(script.getScriptInfo().getStartTime().isPresent());
    assertTrue(script.getScriptInfo().getFinishTime().isPresent());
    assertTrue(script.getScriptInfo().getMessage().isEmpty());
    assertTrue(script.getScriptInfo().getStackTrace().isEmpty());
  }

  @Test
  @SneakyThrows
  public void shouldPassOnThreadInterruption() {
    createScript(INFINITE_SCRIPT);
    Thread thread = new Thread(() -> script.executeScript());
    thread.start();
    Thread.sleep(50);
    thread.interrupt();
    thread.join();
    assertEquals(SCRIPT_ID, script.getId());
    assertEquals(ScriptStatus.CANCELLED, script.getStatus());
    assertArrayEquals(INFINITE_SCRIPT, script.getSource());
    assertArrayEquals(new byte[0], script.getOutput());
    assertEquals(SCRIPT_ID, script.getScriptInfo().getId());
    assertEquals(ScriptStatus.CANCELLED, script.getScriptInfo().getStatus());
    assertNotNull(script.getScriptInfo().getCreateTime());
    assertTrue(script.getScriptInfo().getStartTime().isPresent());
    assertTrue(script.getScriptInfo().getFinishTime().isPresent());
    assertTrue(script.getScriptInfo().getMessage().isEmpty());
    assertTrue(script.getScriptInfo().getStackTrace().isEmpty());
  }

  //  cancelExec

  @Test
  @SneakyThrows
  public void shouldPassOnCancellationExecWithRunningStatus() {
    createScript(INFINITE_SCRIPT);
    executeAsync();
    Thread.sleep(30);
    script.cancelExecution();
    await();
    assertEquals(SCRIPT_ID, script.getId());
    assertEquals(ScriptStatus.CANCELLED, script.getStatus());
    assertArrayEquals(INFINITE_SCRIPT, script.getSource());
    assertArrayEquals(new byte[0], script.getOutput());
    assertEquals(SCRIPT_ID, script.getScriptInfo().getId());
    assertEquals(ScriptStatus.CANCELLED, script.getScriptInfo().getStatus());
    assertNotNull(script.getScriptInfo().getCreateTime());
    assertTrue(script.getScriptInfo().getStartTime().isPresent());
    assertTrue(script.getScriptInfo().getFinishTime().isPresent());
    assertTrue(script.getScriptInfo().getMessage().isEmpty());
    assertTrue(script.getScriptInfo().getStackTrace().isEmpty());
  }

  @Test
  public void shouldPassOnCancellationExecWithQueueStatus() {
    createScript(FINITE_SCRIPT);
    script.cancelExecution();
    script.executeScript();
    assertEquals(SCRIPT_ID, script.getId());
    assertEquals(ScriptStatus.CANCELLED, script.getStatus());
    assertArrayEquals(FINITE_SCRIPT, script.getSource());
    assertArrayEquals(new byte[0], script.getOutput());
    assertEquals(SCRIPT_ID, script.getScriptInfo().getId());
    assertEquals(ScriptStatus.CANCELLED, script.getScriptInfo().getStatus());
    assertNotNull(script.getScriptInfo().getCreateTime());
    assertTrue(script.getScriptInfo().getStartTime().isPresent());
    assertTrue(script.getScriptInfo().getFinishTime().isPresent());
    assertTrue(script.getScriptInfo().getMessage().isEmpty());
    assertTrue(script.getScriptInfo().getStackTrace().isEmpty());
  }

  @Test
  public void shouldPassOnCancellationExecWithFinishedStatus() {
    createScript(FINITE_SCRIPT);
    script.executeScript();
    script.cancelExecution();
    assertEquals(SCRIPT_ID, script.getId());
    assertEquals(ScriptStatus.DONE, script.getStatus());
    assertArrayEquals(FINITE_SCRIPT, script.getSource());
    assertArrayEquals(FINITE_SCRIPT_RESULT, script.getOutput());
    assertEquals(SCRIPT_ID, script.getScriptInfo().getId());
    assertEquals(ScriptStatus.DONE, script.getScriptInfo().getStatus());
    assertNotNull(script.getScriptInfo().getCreateTime());
    assertTrue(script.getScriptInfo().getStartTime().isPresent());
    assertTrue(script.getScriptInfo().getFinishTime().isPresent());
    assertTrue(script.getScriptInfo().getMessage().isEmpty());
    assertTrue(script.getScriptInfo().getStackTrace().isEmpty());
  }
}
