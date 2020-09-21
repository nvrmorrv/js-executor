package impl.repositories;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import impl.repositories.entities.Execution;
import impl.repositories.entities.ExecStatus;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

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
    Optional<Execution> res = repo.removeExecution(id);
    exec = repo.getExecution(id);
    assertTrue(res.isPresent());
    assertEquals(EXECUTION, res.get());
    assertTrue(exec.isEmpty());
  }

  @Test
  public void shouldFailOnRemovingExecByUnknownId() {
    Optional<Execution> res = repo.removeExecution("id");
    assertTrue(res.isEmpty());
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
