package impl.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import impl.repositories.ExecRepository;
import impl.repositories.entities.ExecStatus;
import impl.repositories.entities.Execution;
import impl.service.dto.ExecInfo;
import impl.service.exceptions.DeletionException;
import impl.service.exceptions.ExceptResException;
import impl.service.exceptions.SyntaxErrorException;
import impl.repositories.exceptions.UnknownIdException;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ScriptExecServiceImplTest {
  private static ScriptExecServiceImpl service;
  private final String SCRIPT = "console.log('hello')";
  private final String SCRIPT_ID = "id";
  private Execution execution;
  private final SyntaxErrorException SYN_ERR_EXCEPTION = new SyntaxErrorException("", "");
  private final ExceptResException EXCEPTION_RES_EXCEPTION = new ExceptResException("");
  private final ByteArrayOutputStream stream = new ByteArrayOutputStream();

  @Mock
  public ScriptExecutor executor;

  @Mock
  public ExecRepository repo;

  @BeforeEach
  public void setup() {
    service = new ScriptExecServiceImpl(repo, executor);
    execution = new Execution(
          SCRIPT,
          new AtomicReference<>(ExecStatus.QUEUE),
          new ByteArrayOutputStream(),
          new CompletableFuture<>(),
          new CompletableFuture<>()
    );
  }

  private String getStatus(Execution exec) {
    return exec.getStatus().get().name();
  }

  private String getOutput(Execution exec) {
    return exec.getOutputStream().toString();
  }

  //      executeScriptAsync

  @Test
  public void shouldPassOnAsyncExec() {
    Mockito.when(repo.addExecution(Mockito.any())).thenReturn(SCRIPT_ID);
    assertEquals(SCRIPT_ID, service.executeScriptAsync(SCRIPT));
  }

  @Test
  public void shouldFailOnAsyncExecWithSyntaxErrorScript() {
    Mockito.doThrow(SYN_ERR_EXCEPTION).when(executor).checkScript(SCRIPT);
    assertThatThrownBy(() -> service.executeScriptAsync(SCRIPT))
          .isInstanceOf(SyntaxErrorException.class);
  }

  //    createExec

  @Test
  public void shouldPassOnCreatingExec() {
    Mockito.when(repo.addExecution(Mockito.any())).thenReturn(SCRIPT_ID);
    service.createExec(SCRIPT, stream);
    assertEquals(SCRIPT_ID, service.createExec(SCRIPT, stream));
  }

  @Test
  public void shouldFailOnCreatingExecWithSyntaxError() {
    Mockito.doThrow(SYN_ERR_EXCEPTION).when(executor).checkScript(SCRIPT);
    assertThatThrownBy(() -> service.createExec(SCRIPT, stream))
          .isInstanceOf(SyntaxErrorException.class);
  }

  //    executeScript

  @Test
  public void shouldPassOnBlockingExec() {
    execution.getStatus().set(ExecStatus.CREATED);
    Mockito.when(repo.getExecution(SCRIPT_ID)).thenReturn(Optional.of(execution));
    assertThatCode(() -> service.executeScript(SCRIPT_ID))
          .doesNotThrowAnyException();
  }

  @Test
  public void shouldFailOnBlockingExecWithNotCreatedStatus() {
    Mockito.when(repo.getExecution(SCRIPT_ID)).thenReturn(Optional.of(execution));
    assertThatThrownBy(() -> service.executeScript(SCRIPT_ID))
          .isInstanceOf(IllegalArgumentException.class);
  }

  //  cancelExecution

  @Test
  public void shouldPassOnCancellation() {
    Mockito.when(repo.getExecution(SCRIPT_ID))
          .thenReturn(Optional.of(execution));
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
          .thenReturn(Optional.of(execution));
    assertThatThrownBy(() -> service.deleteExecution(SCRIPT_ID))
          .isInstanceOf(DeletionException.class)
          .hasMessage(DeletionException.generateMessage(SCRIPT_ID));
  }

  @Test
  public void shouldPassOnDeletionOfFinishedExec() {
    execution.getComputation().complete(null);
    Mockito.when(repo.getExecution(SCRIPT_ID))
          .thenReturn(Optional.of(execution));
    Mockito.when(repo.removeExecution(SCRIPT_ID))
          .thenReturn(Optional.of(execution));
    assertThatCode(() -> service.deleteExecution(SCRIPT_ID))
          .doesNotThrowAnyException();
  }

  @Test
  public void shouldFailOnDeletionWithUnknownId() {
    Mockito.when(repo.getExecution(SCRIPT_ID)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.deleteExecution("id"))
          .isInstanceOf(UnknownIdException.class)
          .hasMessage(UnknownIdException.generateMessage(SCRIPT_ID));
  }

  //    getExecutionStatus

  @Test
  public void shouldPassOnGettingStatus() {
    Mockito.when(repo.getExecution(SCRIPT_ID)).thenReturn(Optional.of(execution));
    execution.getComputation().complete(null);
    execution.getStatus().set(ExecStatus.DONE);
    ExecInfo status = service.getExecutionStatus(SCRIPT_ID);
    assertFalse(status.getMessage().isPresent());
    assertEquals(getStatus(execution), status.getStatus());
  }

  @Test
  public void shouldPassOnGettingExceptionStatus() {
    Mockito.when(repo.getExecution(SCRIPT_ID)).thenReturn(Optional.of(execution));
    execution.getComputation().completeExceptionally(EXCEPTION_RES_EXCEPTION);
    ExecInfo status = service.getExecutionStatus(SCRIPT_ID);
    assertTrue(status.getMessage().isPresent());
    assertEquals(ExecStatus.DONE_WITH_EXCEPTION.name(), status.getStatus());
  }

  @Test
  public void shouldFailOnGettingStatusWithUnknownId() {
    Mockito.when(repo.getExecution(SCRIPT_ID)).
          thenThrow(new UnknownIdException(SCRIPT_ID));
    assertThatThrownBy(() -> service.getExecutionStatus(SCRIPT_ID))
          .isInstanceOf(UnknownIdException.class)
          .hasMessage(UnknownIdException.generateMessage(SCRIPT_ID));
  }

  //    getExecutionOutput

  @Test
  public void shouldPassOnGettingOutput() {
    Mockito.when(repo.getExecution(SCRIPT_ID)).thenReturn(Optional.of(execution));
    String output = service.getExecutionOutput(SCRIPT_ID);
    assertEquals(getOutput(execution), output);
  }

  @Test
  public void shouldFailOnGettingOutputWithUnknownId() {
    Mockito.when(repo.getExecution(SCRIPT_ID)).
          thenThrow(new UnknownIdException(SCRIPT_ID));
    assertThatThrownBy(() -> service.getExecutionOutput(SCRIPT_ID))
          .isInstanceOf(UnknownIdException.class)
          .hasMessage(UnknownIdException.generateMessage(SCRIPT_ID));
  }

  //    getExecutionScript

  @Test
  public void shouldPassOnGettingScript() {
    Mockito.when(repo.getExecution(SCRIPT_ID)).thenReturn(Optional.of(execution));
    String script = service.getExecutionScript(SCRIPT_ID);
    assertEquals(execution.getScript(), script);
  }

  @Test
  public void shouldFailOnGettingScriptWithUnknownId() {
    Mockito.when(repo.getExecution(SCRIPT_ID)).
          thenThrow(new UnknownIdException(SCRIPT_ID));
    assertThatThrownBy(() -> service.getExecutionScript(SCRIPT_ID))
          .isInstanceOf(UnknownIdException.class)
          .hasMessage(UnknownIdException.generateMessage(SCRIPT_ID));
  }

  //    getAllExecutionIds

  @Test
  public void shouldPassOnGettingExecIds() {
    Mockito.when(repo.getAllIds()).thenReturn(Set.of(SCRIPT_ID));
    List<String> list = service.getExecutionIds();
    assertEquals(1, list.size());
    assertEquals(SCRIPT_ID, list.get(0));
  }
}
