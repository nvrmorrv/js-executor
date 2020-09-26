package impl.repositories;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import impl.repositories.entities.Script;
import java.util.List;

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
  private final String SCRIPT_ID = "id";

  @Mock
  Script script;

  @Mock
  MeterRegistry registry;

  @BeforeEach
  public void setup() {
    repo = new ScriptRepositoryImpl(registry);
  }

  @Test
  public void shouldPassOnAddingScript() {
    boolean created = repo.addOrUpdateScript(SCRIPT_ID, script);
    Script resScript = repo.getScript(SCRIPT_ID);
    assertTrue(created);
    assertEquals(script, resScript);
  }

  @Test
  public void shouldPassOnUpdatingScript() {
    boolean created = repo.addOrUpdateScript(SCRIPT_ID, script);
    Script resScript = repo.getScript(SCRIPT_ID);
    assertTrue(created);
    assertEquals(script, resScript);
    created = repo.addOrUpdateScript(SCRIPT_ID, script);
    resScript = repo.getScript(SCRIPT_ID);
    assertFalse(created);
    assertEquals(script, resScript);
  }

  @Test
  public void shouldPassOnGettingScript() {
    repo.addOrUpdateScript(SCRIPT_ID, script);
    Script resScript = repo.getScript(SCRIPT_ID);
    assertEquals(script, resScript);
  }

  @Test
  public void shouldFailOnGettingScriptByUnknownId() {
    assertThatThrownBy(() -> repo.getScript("id"))
          .isInstanceOf(UnknownIdException.class)
          .hasMessage(UnknownIdException.generateMessage("id"));
  }

  @Test
  public void shouldPassOnRemovingScript() {
    repo.addOrUpdateScript(SCRIPT_ID, script);
    Script resScript = repo.getScript(SCRIPT_ID);
    assertEquals(script, resScript);
    repo.removeScript(SCRIPT_ID);
    assertThatThrownBy(() -> repo.getScript(SCRIPT_ID))
          .isInstanceOf(UnknownIdException.class)
          .hasMessage(UnknownIdException.generateMessage(SCRIPT_ID));
  }

  @Test
  public void shouldFailOnRemovingScriptByUnknownId() {
    assertThatThrownBy(() -> repo.getScript("id"))
          .isInstanceOf(UnknownIdException.class)
          .hasMessage(UnknownIdException.generateMessage("id"));
  }

  @Test
  public void shouldPassOnGettingAllExecIds() {
    repo.addOrUpdateScript(SCRIPT_ID, script);
    repo.addOrUpdateScript(SCRIPT_ID + 1, script);
    List<Script> scripts = repo.getScripts();
    assertEquals(2, scripts.size());
    assertEquals(script, scripts.get(0));
    assertEquals(script, scripts.get(1));
  }
}
