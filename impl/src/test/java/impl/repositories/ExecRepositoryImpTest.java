package impl.repositories;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import impl.repositories.entities.Execution;
import impl.service.ExecStatus;
import impl.service.exceptions.UnknownIdException;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExecRepositoryImpTest {
  private ExecRepositoryImpl repo;
  private final Execution EXECUTION = new Execution(
        "script",
        new AtomicReference<>(ExecStatus.QUEUE),
        new ByteArrayOutputStream(),
        new CompletableFuture<>(),
        new CompletableFuture<>()
  );

  @BeforeEach
  public void setup() {
    repo = new ExecRepositoryImpl();
  }

  @Test
  public void shouldPassOnAddingExec() {
    String id = repo.addExecution(EXECUTION);
    Optional<Execution> exec = repo.getExecution(id);
    assertTrue(exec.isPresent());
    assertEquals(EXECUTION, exec.get());
  }

  @Test
  public void shouldPassOnGettingExec() {
    String id = repo.addExecution(EXECUTION);
    Optional<Execution> exec = repo.getExecution(id);
    assertTrue(exec.isPresent());
    assertEquals(EXECUTION, exec.get());
  }

  @Test
  public void shouldFailOnGettingExecByUnknownId() {
    Optional<Execution> exec = repo.getExecution("id");
    assertTrue(exec.isEmpty());
  }

  @Test
  public void shouldPassOnRemovingExec() {
    String id = repo.addExecution(EXECUTION);
    Optional<Execution> exec = repo.getExecution(id);
    assertTrue(exec.isPresent());
    assertEquals(EXECUTION, exec.get());
    repo.removeExecution(id);
    exec = repo.getExecution(id);
    assertTrue(exec.isEmpty());
  }

  @Test
  public void shouldFailOnRemovingExecByUnknownId() {
    assertThatThrownBy(() -> repo.removeExecution("id"))
          .isInstanceOf(UnknownIdException.class);
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
