package impl.repositories;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import impl.repositories.entities.Script;
import impl.shared.ScriptStatus;
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
public class ScriptRepositoryImpTest {
  private ScriptRepositoryImpl repo;
  private final Script Script = new Script(
        "script",
        new AtomicReference<>(ScriptStatus.QUEUE),
        new ByteArrayOutputStream(),
        new CompletableFuture<>(),
        new CompletableFuture<>()
  );

  @Mock
  MeterRegistry registry;

  @BeforeEach
  public void setup() {
    repo = new ScriptRepositoryImpl(registry);
  }

  @Test
  public void shouldPassOnAddingExec() {
    String id = repo.addScript(Script);
    Script exec = repo.getScript(id);
    assertEquals(Script, exec);
  }

  @Test
  public void shouldPassOnGettingExec() {
    String id = repo.addScript(Script);
    Script exec = repo.getScript(id);
    assertEquals(Script, exec);
  }

  @Test
  public void shouldFailOnGettingExecByUnknownId() {
    assertThatThrownBy(() -> repo.getScript("id"))
          .isInstanceOf(UnknownIdException.class)
          .hasMessage(UnknownIdException.generateMessage("id"));
  }

  @Test
  public void shouldPassOnRemovingExec() {
    String id = repo.addScript(Script);
    Script exec = repo.getScript(id);
    assertEquals(Script, exec);
    repo.removeScript(id);
    assertThatThrownBy(() -> repo.getScript(id))
          .isInstanceOf(UnknownIdException.class)
          .hasMessage(UnknownIdException.generateMessage(id));
  }

  @Test
  public void shouldFailOnRemovingExecByUnknownId() {
    assertThatThrownBy(() -> repo.getScript("id"))
          .isInstanceOf(UnknownIdException.class)
          .hasMessage(UnknownIdException.generateMessage("id"));
  }

  @Test
  public void shouldPassOnGettingAllExecIds() {
    String id = repo.addScript(Script);
    String id1 = repo.addScript(Script);
    Set<String> ids = repo.getAllIds();
    assertEquals(2, ids.size());
    assertTrue(ids.containsAll(Arrays.asList(id, id1)));
  }
}
