package impl.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import impl.repositories.ScriptRepository;
import impl.shared.ScriptInfo;
import impl.shared.ScriptStatus;
import impl.repositories.entities.Script;
import impl.service.exceptions.DeletionException;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class ScriptExecServiceImplTest {
  private static ScriptExecServiceImpl service;
  private final byte[] SOURCE = "console.log('hello')".getBytes();
  private final String SCRIPT_ID = "id";
  private final TimeZone TIME_ZONE = TimeZone.getDefault();
  private final ByteArrayOutputStream stream = new ByteArrayOutputStream();

  @Mock
  ScriptRepository repo;

  @Mock
  Script mockScript;

  @Captor
  ArgumentCaptor<OutputStream> executeScriptCaptor;

  @BeforeEach
  public void setup() {
    service = new ScriptExecServiceImpl("js", repo);
  }

  @Test
  public void shouldPassOnCreatingScript() {
    Mockito.when(repo.contains(SCRIPT_ID)).thenReturn(false);
    Mockito.when(repo.addOrUpdateScript(Mockito.eq(SCRIPT_ID), Mockito.any())).thenReturn(true);
    assertTrue(service.createScript(SCRIPT_ID, SOURCE, TIME_ZONE));
  }

  @Test
  public void shouldPassOnUpdatingScript() {
    Mockito.when(repo.contains(SCRIPT_ID)).thenReturn(true);
    Mockito.when(repo.getScript(SCRIPT_ID)).thenReturn(mockScript);
    Mockito.when(mockScript.getStatus()).thenReturn(ScriptStatus.DONE);
    Mockito.when(repo.addOrUpdateScript(Mockito.eq(SCRIPT_ID), Mockito.any())).thenReturn(false);
    assertFalse(service.createScript(SCRIPT_ID, SOURCE, TIME_ZONE));
  }

  @Test
  public void shouldFailOnUpdatingNotFinishedScript() {
    Mockito.when(repo.contains(SCRIPT_ID)).thenReturn(true);
    Mockito.when(repo.getScript(SCRIPT_ID)).thenReturn(mockScript);
    Mockito.when(mockScript.getStatus()).thenReturn(ScriptStatus.RUNNING);
    assertThatThrownBy(() -> service.createScript(SCRIPT_ID, SOURCE, TIME_ZONE))
          .isInstanceOf(DeletionException.class);
  }

  @Test
  public void shouldPassOnAsyncExec() {
    Mockito.when(repo.getScript(SCRIPT_ID)).thenReturn(mockScript);
    service.executeScriptAsync(SCRIPT_ID);
    Mockito.verify(mockScript, Mockito.only()).executeScript();
  }

  @Test
  public void shouldPassOnBlockingExec() {
    Mockito.when(repo.getScript(SCRIPT_ID)).thenReturn(mockScript);
    service.executeScript(SCRIPT_ID, stream);
    Mockito.verify(mockScript, Mockito.only()).executeScript(executeScriptCaptor.capture());
    assertEquals(stream, executeScriptCaptor.getValue());
  }

  @Test
  public void shouldPassOnCancellation() {
    Mockito.when(repo.getScript(SCRIPT_ID)).thenReturn(mockScript);
    service.cancelScriptExecution(SCRIPT_ID);
    Mockito.verify(mockScript, Mockito.only()).cancelExecution();
  }

  //    deleteExecution

  @Test
  public void shouldFailOnDeletionOfNotFinishedScript() {
    Mockito.when(repo.getScript(SCRIPT_ID)).thenReturn(mockScript);
    Mockito.when(mockScript.getStatus()).thenReturn(ScriptStatus.RUNNING);
    assertThatThrownBy(() -> service.deleteScript(SCRIPT_ID))
          .isInstanceOf(DeletionException.class);
  }

  @Test
  public void shouldPassOnDeletionOfFinishedScript() {
    Mockito.when(repo.getScript(SCRIPT_ID)).thenReturn(mockScript);
    Mockito.when(mockScript.getStatus()).thenReturn(ScriptStatus.DONE);
    assertThatCode(() -> service.deleteScript(SCRIPT_ID))
          .doesNotThrowAnyException();
  }

  @Test
  public void shouldPassOnGettingScriptOutput() {
    byte[] output = new byte[0];
    Mockito.when(repo.getScript(SCRIPT_ID)).thenReturn(mockScript);
    Mockito.when(mockScript.getOutput()).thenReturn(output);
    assertEquals(output, service.getScriptOutput(SCRIPT_ID));
  }

  @Test
  public void shouldPassOnGettingScriptSource() {
    Mockito.when(repo.getScript(SCRIPT_ID)).thenReturn(mockScript);
    Mockito.when(mockScript.getSource()).thenReturn(SOURCE);
    assertEquals(SOURCE, service.getScriptSource(SCRIPT_ID));
  }

  @Test
  public void shouldPassOnGettingScriptInfo() {
    ScriptInfo info = new ScriptInfo(SCRIPT_ID, ScriptStatus.RUNNING, ZonedDateTime.now(),
          Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    Mockito.when(repo.getScript(SCRIPT_ID)).thenReturn(mockScript);
    Mockito.when(mockScript.getScriptInfo()).thenReturn(info);
    assertEquals(info, service.getScriptInfo(SCRIPT_ID));
  }

  @Test
  public void shouldPassOnGettingScriptInfoPage() {
    int size = 1;
    int page = 0;
    Pageable pageable = PageRequest.of(page, size);
    String filterStatus = "ANY";
    ScriptInfo info = new ScriptInfo(SCRIPT_ID, ScriptStatus.RUNNING, ZonedDateTime.now(),
          Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    Mockito.when(repo.getScripts()).thenReturn(Collections.singletonList(mockScript));
    Mockito.when(mockScript.getScriptInfo()).thenReturn(info);
    Page<ScriptInfo> resPage = service.getScriptInfoPage(pageable, filterStatus);
    assertEquals(1, resPage.getTotalElements());
    assertEquals(1, resPage.getTotalPages());
    assertEquals(size, resPage.getSize());
    assertEquals(page, resPage.getNumber());
  }
}
