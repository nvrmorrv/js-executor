package impl.repositories;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import impl.repositories.entities.Execution;
import impl.repositories.entities.ExecStatus;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import impl.repositories.exceptions.UnknownIdException;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ExecRepositoryImpTest {
  private ExecRepositoryImpl repo;
  private final Execution EXECUTION = new Execution(
        "script",
        new AtomicReference<>(ExecStatus.QUEUE),
        new ByteArrayOutputStream(),
        new CompletableFuture<>(),
        new CompletableFuture<>()
  );

  @Mock
  MeterRegistry registry;

  @BeforeEach
  public void setup() {
    repo = new ExecRepositoryImpl(registry);
  }

  @Test
  public void shouldPassOnAddingExec() {
    String id = repo.addExecution(EXECUTION);
    Execution exec = repo.getExecution(id);
    assertEquals(EXECUTION, exec);
  }

  @Test
  public void shouldPassOnGettingExec() {
    String id = repo.addExecution(EXECUTION);
    Execution exec = repo.getExecution(id);
    assertEquals(EXECUTION, exec);
  }

  @Test
  public void shouldFailOnGettingExecByUnknownId() {
    assertThatThrownBy(() -> repo.getExecution("id"))
          .isInstanceOf(UnknownIdException.class)
          .hasMessage(UnknownIdException.generateMessage("id"));
  }

  @Test
  public void shouldPassOnRemovingExec() {
    String id = repo.addExecution(EXECUTION);
    Execution exec = repo.getExecution(id);
    assertEquals(EXECUTION, exec);
    repo.removeExecution(id);
    assertThatThrownBy(() -> repo.getExecution(id))
          .isInstanceOf(UnknownIdException.class)
          .hasMessage(UnknownIdException.generateMessage(id));
  }

  @Test
  public void shouldFailOnRemovingExecByUnknownId() {
    assertThatThrownBy(() -> repo.getExecution("id"))
          .isInstanceOf(UnknownIdException.class)
          .hasMessage(UnknownIdException.generateMessage("id"));
  }

  @Test
  public void shouldPassOnGettingAllExecIds() {
    String id = repo.addExecution(EXECUTION);
    String id1 = repo.addExecution(EXECUTION);
    Set<String> ids = repo.getAllIds();
    assertEquals(2, ids.size());
    assertTrue(ids.containsAll(Arrays.asList(id, id1)));
  }
}
