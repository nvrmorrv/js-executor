package impl.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import impl.repositories.ExecRepository;
import impl.repositories.entities.Execution;
import impl.service.dto.ExecInfo;
import impl.service.exceptions.DeletionException;
import impl.service.exceptions.ExecTimeOutException;
import impl.service.exceptions.UnknownIdException;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ScriptExecServiceImplTest {
  private ScriptExecServiceImpl service;
  private final String SCRIPT = "console.log('hello')";
  private final String SCRIPT_ID = "id";
  private final long TIMEOUT = 1;
  private final TimeUnit TIME_UNIT = TimeUnit.MINUTES;
  private final Execution EXECUTION = new Execution(
        new AtomicReference<>(ExecStatus.QUEUE),
        new ByteArrayOutputStream(),
        new CompletableFuture<>(),
        new CompletableFuture<>()
  );

  @Mock
  public ScriptExecutor executor;

  @Mock
  public ExecRepository repo;

  @BeforeEach
  public void setup() {
    service = new ScriptExecServiceImpl(repo, executor);
    EXECUTION.getStatus().set(ExecStatus.QUEUE);
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
    Mockito.when(executor.executeAsync(SCRIPT)).thenReturn(EXECUTION);
    Mockito.when(repo.addExecution(EXECUTION)).thenReturn(SCRIPT_ID);
    assertEquals(SCRIPT_ID, service.executeScriptAsync(SCRIPT));
  }

  //    executeScript

  @Test
  public void shouldPassOnBlockingExecWithGettingStatus()
        throws InterruptedException, ExecutionException, TimeoutException {
    Mockito.when(executor.execute(SCRIPT, TIMEOUT, TIME_UNIT)).thenReturn(EXECUTION);
    ExecInfo res = service.executeScript(SCRIPT, TIMEOUT, TIME_UNIT);
    assertEquals(getStatus(EXECUTION), res.getStatus());
    assertEquals(getOutput(EXECUTION), res.getOutput());
  }

  @Test
  public void shouldFailOnBlockingExecWhenTimeout()
        throws InterruptedException, ExecutionException, TimeoutException {
    Mockito.when(executor.execute(SCRIPT, TIMEOUT, TIME_UNIT)).
          thenThrow(new TimeoutException());
    assertThatThrownBy(() ->
          service.executeScript(SCRIPT, TIMEOUT, TIME_UNIT))
          .isInstanceOf(ExecTimeOutException.class)
          .hasMessage(ExecTimeOutException.generateMessage(TIMEOUT, TIME_UNIT));
  }

  //  cancelExecution

  @Test
  public void shouldPassOnCancellation() {
    Mockito.when(repo.getExecution(SCRIPT_ID))
          .thenReturn(Optional.of(EXECUTION));
    assertThatCode(() -> service.cancelExecution(SCRIPT_ID))
          .doesNotThrowAnyException();
  }

  @Test
  public void shouldFailOnCancellationWithUnknownId() {
    Mockito.when(repo.getExecution(SCRIPT_ID)).
          thenThrow(new UnknownIdException(SCRIPT_ID));
    assertThatThrownBy(() -> service.getExecutionStatus(SCRIPT_ID))
          .isInstanceOf(UnknownIdException.class)
          .hasMessage(UnknownIdException.generateMessage(SCRIPT_ID));
  }

  //    deleteExecution

  @Test
  public void shouldFailOnDeletionOfNotCancelledExec() {
    Mockito.when(repo.getExecution(SCRIPT_ID))
          .thenReturn(Optional.of(EXECUTION));
    assertThatThrownBy(() -> service.deleteExecution(SCRIPT_ID))
          .isInstanceOf(DeletionException.class)
          .hasMessage(DeletionException.generateMessage(SCRIPT_ID));
  }

  @Test
  public void shouldPassOnDeletionOfCancelledExec() {
    EXECUTION.getStatus().set(ExecStatus.DONE);
    Mockito.when(repo.getExecution(SCRIPT_ID))
          .thenReturn(Optional.of(EXECUTION));
    assertThatCode(() -> service.deleteExecution(SCRIPT_ID))
          .doesNotThrowAnyException();
  }

  @Test
  public void shouldFailOnDeletionWithUnknownId() {
    Mockito.when(repo.getExecution(SCRIPT_ID)).
          thenThrow(new UnknownIdException(SCRIPT_ID));
    assertThatThrownBy(() -> service.deleteExecution("id"))
          .isInstanceOf(UnknownIdException.class)
          .hasMessage(UnknownIdException.generateMessage(SCRIPT_ID));
  }

  //    getExecutionStatus

  @Test
  public void shouldPassOnGettingStatus() {
    Mockito.when(repo.getExecution(SCRIPT_ID)).
          thenReturn(Optional.of(EXECUTION));
    ExecInfo status = service.getExecutionStatus(SCRIPT_ID);
    assertEquals(getStatus(EXECUTION), status.getStatus());
    assertEquals(getOutput(EXECUTION), status.getOutput());
  }

  @Test
  public void shouldFailOnGettingStatusWithUnknownId() {
    Mockito.when(repo.getExecution(SCRIPT_ID)).
          thenThrow(new UnknownIdException(SCRIPT_ID));
    assertThatThrownBy(() -> service.getExecutionStatus(SCRIPT_ID))
          .isInstanceOf(UnknownIdException.class)
          .hasMessage(UnknownIdException.generateMessage(SCRIPT_ID));
  }

  //    getAllExecutionIds

  @Test
  public void shouldPassOnGettingExecIds() {
    Mockito.when(repo.getAllIds()).thenReturn(Set.of(SCRIPT_ID));
    List<String> list = service.getAllExecutionIds();
    assertEquals(1, list.size());
    assertEquals(SCRIPT_ID, list.get(0));
  }

  //    getAllFinishedExecutionIds

  @Test
  public void shouldPassOnGettingFinishedExecIds() {
    Mockito.when(repo.getAllIds()).thenReturn(Set.of(SCRIPT_ID));
    Mockito.when(repo.getExecution(SCRIPT_ID)).
          thenReturn(Optional.of(EXECUTION));
    List<String> list = service.getFinishedExecutionIds();
    assertEquals(0, list.size());
  }

}
