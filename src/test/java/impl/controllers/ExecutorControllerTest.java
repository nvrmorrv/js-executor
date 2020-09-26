package impl.controllers;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import impl.controllers.dto.CancelReq;
import impl.shared.ScriptInfo;
import impl.shared.ScriptStatus;
import impl.service.ScriptExecService;
import impl.service.exceptions.DeletionException;
import impl.service.exceptions.SyntaxErrorException;
import impl.repositories.exceptions.UnknownIdException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ExecutorController.class)
@AutoConfigureMockMvc
public class ExecutorControllerTest {
  private final String SCRIPT_ID = "id";
  private final String SCRIPT = "console.log('hello')";
  ScriptInfo runningInfo = new ScriptInfo(
        SCRIPT_ID,
        ScriptStatus.RUNNING,
        ZonedDateTime.now(),
        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  ScriptInfo doneInfo = new ScriptInfo(
        SCRIPT_ID, ScriptStatus.DONE,
        ZonedDateTime.now(),
        Optional.of(ZonedDateTime.now().plusMinutes(1)),
        Optional.of(ZonedDateTime.now().plusMinutes(2)),
        Optional.empty(), Optional.empty());
  ScriptInfo exceptionInfo = new ScriptInfo(
        SCRIPT_ID, ScriptStatus.DONE_WITH_EXCEPTION,
        ZonedDateTime.now(),
        Optional.of(ZonedDateTime.now().plusMinutes(1)),
        Optional.of(ZonedDateTime.now().plusMinutes(2)),
        Optional.of("exception"),
        Optional.of(Collections.emptyList()));
  private final SyntaxErrorException SYN_ERR_EXCEPTION = new SyntaxErrorException("syntax error", "here");

  @Autowired
  private MockMvc mvc;

  @MockBean
  private ScriptExecService service;

  @Autowired
  private ObjectMapper mapper;

  private String getStringFromDate(Optional<ZonedDateTime> dateTime) {
    return dateTime.map(this::getStringFromDate).orElse("");
  }

  private String getStringFromDate(ZonedDateTime dateTime) {
    return dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss:SSS;dd-MM-uuuu;O"));
  }

  @Test
  public void shouldPassOnGettingRoot() throws Exception {
    mvc.perform(get("/"))
          .andExpect(status().is(200))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/")))
          .andExpect(jsonPath("$._links.scripts.href", Matchers.containsString("/scripts")));
  }

  @Test
  public void shouldPassOnGettingPageWithDoneScript() throws Exception {
    Mockito.when(service.getScriptInfoPage(Mockito.any(), Mockito.any()))
          .thenReturn(new PageImpl<>(
                Collections.singletonList(doneInfo), PageRequest.of(0, 2), 1));
    mvc.perform(get("/scripts"))
          .andExpect(status().is(200))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$._embedded.scripts.[0].id", Matchers.is(SCRIPT_ID)))
          .andExpect(jsonPath("$._embedded.scripts.[0].status", Matchers.is(doneInfo.getStatus().name())))
          .andExpect(jsonPath("$._embedded.scripts.[0].createTime",
                Matchers.is(getStringFromDate(doneInfo.getCreateTime()))))
          .andExpect(jsonPath("$._embedded.scripts.[0].startTime",
                Matchers.is(getStringFromDate(doneInfo.getStartTime()))))
          .andExpect(jsonPath("$._embedded.scripts.[0].finishTime",
                Matchers.is(getStringFromDate(doneInfo.getFinishTime()))));
  }

  @Test
  public void shouldPassOnGettingPageWithRunningScript() throws Exception {
    Mockito.when(service.getScriptInfoPage(Mockito.any(), Mockito.any()))
          .thenReturn(new PageImpl<>(
                Collections.singletonList(runningInfo), PageRequest.of(0, 2), 1));
    mvc.perform(get("/scripts"))
          .andExpect(status().is(200))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$._embedded.scripts.[0].id", Matchers.is(SCRIPT_ID)))
          .andExpect(jsonPath("$._embedded.scripts.[0].status",
                Matchers.is(runningInfo.getStatus().name())))
          .andExpect(jsonPath("$._embedded.scripts.[0].createTime",
                Matchers.is(getStringFromDate(runningInfo.getCreateTime()))))
          .andExpect(jsonPath("$._embedded.scripts.[0].startTime",
                Matchers.is(getStringFromDate(runningInfo.getStartTime()))))
          .andExpect(jsonPath("$._embedded.scripts.[0].finishTime",
                Matchers.is(getStringFromDate(runningInfo.getFinishTime()))));
  }

  @Test
  public void shouldPassOnGettingPage() throws Exception {
    Mockito.when(service.getScriptInfoPage(Mockito.any(), Mockito.any()))
          .thenReturn(new PageImpl<>(
                Collections.singletonList(runningInfo), PageRequest.of(0, 1), 2));
    mvc.perform(get("/scripts"))
          .andExpect(status().is(200))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.page.size", Matchers.is(1)))
          .andExpect(jsonPath("$.page.number", Matchers.is(0)))
          .andExpect(jsonPath("$.page.totalPages", Matchers.is(2)))
          .andExpect(jsonPath("$.page.totalElements", Matchers.is(2)))
          .andExpect(jsonPath("$._links.next.href", Matchers.containsString("page=1")))
          .andExpect(jsonPath("$._links.last.href", Matchers.containsString("page=1")))
          .andExpect(jsonPath("$._links.first.href", Matchers.containsString("page=0")));
  }

  @Test
  public void shouldPassOnGettingEmptyPage() throws Exception {
    Mockito.when(service.getScriptInfoPage(Mockito.any(), Mockito.any()))
          .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 1), 0));
    mvc.perform(get("/scripts"))
          .andExpect(status().is(200))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/scripts")))
          .andExpect(jsonPath("$._links.blocking.href", Matchers.containsString("/scripts/{id}?blocking=true")))
          .andExpect(jsonPath("$._links.blocking.type", Matchers.is("PUT")))
          .andExpect(jsonPath("$._links.blocking.templated", Matchers.is(true)))
          .andExpect(jsonPath("$._links.async.href", Matchers.containsString("/scripts/{id}?blocking=false")))
          .andExpect(jsonPath("$._links.async.type", Matchers.is("PUT")))
          .andExpect(jsonPath("$._links.async.templated", Matchers.is(true)));
  }

  @Test
  public void shouldPassOnAsyncScriptExecutionAndCreating() throws Exception {
    Mockito.when(service.createScript(Mockito.eq(SCRIPT_ID), Mockito.eq(SCRIPT.getBytes()), Mockito.any())).thenReturn(true);
    mvc.perform(put("/scripts/" + SCRIPT_ID)
          .content(SCRIPT)
          .queryParam("blocking", "false")
          .contentType(MediaType.TEXT_PLAIN))
          .andExpect(status().is(201))
          .andExpect(header().exists("Location"))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id", Matchers.is(SCRIPT_ID)))
          .andExpect(jsonPath("$._links.self.href", Matchers.containsString(SCRIPT_ID)));
  }

  @Test
  public void shouldPassOnAsyncScriptExecutionAndCreatingByDefault() throws Exception {
    Mockito.when(service.createScript(Mockito.eq(SCRIPT_ID), Mockito.eq(SCRIPT.getBytes()), Mockito.any())).thenReturn(true);
    mvc.perform(put("/scripts/" + SCRIPT_ID)
          .content(SCRIPT)
          .contentType(MediaType.TEXT_PLAIN))
          .andExpect(status().is(201))
          .andExpect(header().exists("Location"))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id", Matchers.is(SCRIPT_ID)))
          .andExpect(jsonPath("$._links.self.href", Matchers.containsString(SCRIPT_ID)));
  }

  @Test
  public void shouldPassOnAsyncScriptExecutionAndUpdating() throws Exception {
    Mockito.when(service.createScript(Mockito.eq(SCRIPT_ID), Mockito.eq(SCRIPT.getBytes()), Mockito.any())).thenReturn(false);
    mvc.perform(put("/scripts/" + SCRIPT_ID)
          .content(SCRIPT)
          .queryParam("blocking", "false")
          .contentType(MediaType.TEXT_PLAIN))
          .andExpect(status().is(200))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id", Matchers.is(SCRIPT_ID)))
          .andExpect(jsonPath("$._links.self.href", Matchers.containsString(SCRIPT_ID)));
  }

  @Test
  public void shouldFailOnAsyncPerformingScriptWithSyntaxError() throws Exception {
    Mockito.when(service.createScript(Mockito.eq(SCRIPT_ID), Mockito.eq(SCRIPT.getBytes()), Mockito.any()))
          .thenThrow(SYN_ERR_EXCEPTION);
    mvc.perform(put("/scripts/" + SCRIPT_ID)
          .content(SCRIPT)
          .queryParam("blocking", "false")
          .contentType(MediaType.TEXT_PLAIN))
          .andExpect(status().is(400))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.title", Matchers.is("Syntax error")))
          .andExpect(jsonPath("$.status", Matchers.is("BAD_REQUEST")))
          .andExpect(jsonPath("$.detail", Matchers.is(SYN_ERR_EXCEPTION.getMessage())));
  }

  @Test
  public void shouldPassOnBlockingScriptExecutionAndCreating() throws Exception {
    Mockito.when(service.createScript(Mockito.eq(SCRIPT_ID), Mockito.eq(SCRIPT.getBytes()), Mockito.any())).thenReturn(true);
    MvcResult result = mvc.perform(put("/scripts/" + SCRIPT_ID)
          .content(SCRIPT)
          .queryParam("blocking", "true")
          .contentType(MediaType.TEXT_PLAIN)).andReturn();
    mvc.perform(asyncDispatch(result))
          .andExpect(status().is(201))
          .andExpect(header().exists("Location"))
          .andExpect(content().contentType(MediaType.TEXT_PLAIN));
  }

  @Test
  public void shouldPassOnBlockingScriptExecutionAndUpdating() throws Exception {
    Mockito.when(service.createScript(Mockito.eq(SCRIPT_ID), Mockito.eq(SCRIPT.getBytes()), Mockito.any())).thenReturn(false);
    MvcResult result = mvc.perform(put("/scripts/" + SCRIPT_ID)
          .content(SCRIPT)
          .queryParam("blocking", "true")
          .contentType(MediaType.TEXT_PLAIN)).andReturn();
    mvc.perform(asyncDispatch(result))
          .andExpect(status().is(200))
          .andExpect(content().contentType(MediaType.TEXT_PLAIN));
  }

  //@Test
  public void shouldFailOnBlockingPerformingScriptWithSyntaxError() throws Exception {
    Mockito.when(service.createScript(Mockito.eq(SCRIPT_ID), Mockito.eq(SCRIPT.getBytes()), Mockito.any()))
          .thenThrow(SYN_ERR_EXCEPTION);
    MvcResult result = mvc.perform(put("/scripts/" + SCRIPT_ID)
          .content(SCRIPT)
          .queryParam("blocking", "true")
          .contentType(MediaType.TEXT_PLAIN)).andReturn();
    mvc.perform(asyncDispatch(result))
          .andExpect(status().is(400))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.title", Matchers.is("Syntax error")))
          .andExpect(jsonPath("$.status", Matchers.is("BAD_REQUEST")))
          .andExpect(jsonPath("$.detail", Matchers.is(SYN_ERR_EXCEPTION.getMessage())));
  }

  @Test
  public void shouldPassOnGettingStatus() throws Exception {
    Mockito.when(service.getScriptInfo(SCRIPT_ID)).thenReturn(doneInfo);
    mvc.perform(
          get("/scripts/" + SCRIPT_ID))
          .andExpect(status().is(200))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id", Matchers.is(doneInfo.getId())))
          .andExpect(jsonPath("$.status", Matchers.is(doneInfo.getStatus().name())))
          .andExpect(jsonPath("$.createTime",
                Matchers.is(getStringFromDate(doneInfo.getCreateTime()))))
          .andExpect(jsonPath("$.startTime",
                Matchers.is(getStringFromDate(doneInfo.getStartTime()))))
          .andExpect(jsonPath("$.finishTime",
                Matchers.is(getStringFromDate(doneInfo.getFinishTime()))))
          .andExpect(jsonPath("$._links.self.href", Matchers.containsString(SCRIPT_ID)))
          .andExpect(jsonPath("$._links.source.href", Matchers.containsString(SCRIPT_ID + "/source")))
          .andExpect(jsonPath("$._links.output.href", Matchers.containsString(SCRIPT_ID + "/output")))
          .andExpect(jsonPath("$._links.cancel.href", Matchers.containsString(SCRIPT_ID)))
          .andExpect(jsonPath("$._links.cancel.type", Matchers.is("PATCH")))
          .andExpect(jsonPath("$._links.delete.href", Matchers.containsString(SCRIPT_ID)))
          .andExpect(jsonPath("$._links.delete.type", Matchers.is("DELETE")))
          .andExpect(jsonPath("$._links.blocking.href", Matchers.containsString(SCRIPT_ID)))
          .andExpect(jsonPath("$._links.blocking.type", Matchers.is("PUT")))
          .andExpect(jsonPath("$._links.async.href", Matchers.containsString(SCRIPT_ID)))
          .andExpect(jsonPath("$._links.async.type", Matchers.is("PUT")));
  }

  @Test
  public void shouldPassOnGettingStatusWhenException() throws Exception {
    Mockito.when(service.getScriptInfo(SCRIPT_ID)).thenReturn(exceptionInfo);
    mvc.perform(
          get("/scripts/" + SCRIPT_ID))
          .andExpect(status().is(200))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id", Matchers.is(exceptionInfo.getId())))
          .andExpect(jsonPath("$.status", Matchers.is(exceptionInfo.getStatus().name())))
          .andExpect(jsonPath("$.createTime",
                Matchers.is(getStringFromDate(exceptionInfo.getCreateTime()))))
          .andExpect(jsonPath("$.startTime",
                Matchers.is(getStringFromDate(exceptionInfo.getStartTime()))))
          .andExpect(jsonPath("$.finishTime",
                Matchers.is(getStringFromDate(exceptionInfo.getFinishTime()))))
          .andExpect(jsonPath("$.message",
                Matchers.is(exceptionInfo.getMessage().get())))
          .andExpect(jsonPath("$.stackTrace",
                Matchers.is(exceptionInfo.getStackTrace().get())))
          .andExpect(jsonPath("$._links.self.href", Matchers.containsString(SCRIPT_ID)))
          .andExpect(jsonPath("$._links.source.href", Matchers.containsString(SCRIPT_ID + "/source")))
          .andExpect(jsonPath("$._links.output.href", Matchers.containsString(SCRIPT_ID + "/output")))
          .andExpect(jsonPath("$._links.cancel.href", Matchers.containsString(SCRIPT_ID)))
          .andExpect(jsonPath("$._links.cancel.type", Matchers.is("PATCH")))
          .andExpect(jsonPath("$._links.delete.href", Matchers.containsString(SCRIPT_ID)))
          .andExpect(jsonPath("$._links.delete.type", Matchers.is("DELETE")))
          .andExpect(jsonPath("$._links.blocking.href", Matchers.containsString(SCRIPT_ID)))
          .andExpect(jsonPath("$._links.blocking.type", Matchers.is("PUT")))
          .andExpect(jsonPath("$._links.async.href", Matchers.containsString(SCRIPT_ID)))
          .andExpect(jsonPath("$._links.async.type", Matchers.is("PUT")));
  }

  @Test
  public void shouldFailOnGettingStatusWithUnknownId() throws Exception {
    Mockito.when(service.getScriptInfo(SCRIPT_ID)).thenThrow(new UnknownIdException(SCRIPT_ID));
    mvc.perform(
          get("/scripts/" + SCRIPT_ID))
          .andExpect(status().is(404))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.title", Matchers.is("Unknown id")))
          .andExpect(jsonPath("$.status", Matchers.is("NOT_FOUND")))
          .andExpect(jsonPath("$.detail", Matchers.is(UnknownIdException.generateMessage(SCRIPT_ID))));
  }

  @Test
  public void shouldPassOnCancellation() throws Exception {
    String content = mapper.writeValueAsString(new CancelReq("CANCELLED"));
    mvc.perform(
          patch("/scripts/" + SCRIPT_ID)
          .content(content)
          .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().is(200))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id", Matchers.is(SCRIPT_ID)))
          .andExpect(jsonPath("$._links.self.href", Matchers.containsString(SCRIPT_ID)));
  }

  @Test
  public void shouldFailOnCancellationWithWrongStatusPassed() throws Exception {
    String content = mapper.writeValueAsString(new CancelReq("QUEUE"));
    mvc.perform(
          patch("/scripts/" + SCRIPT_ID)
                .content(content)
                .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().is(400))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.title", Matchers.is("Passed status not allowed")))
          .andExpect(jsonPath("$.status", Matchers.is("BAD_REQUEST")));
  }

  @Test
  public void shouldFailOnCancellationWithUnknownId() throws Exception {
    String content = mapper.writeValueAsString(new CancelReq("CANCELLED"));
    Mockito.doThrow(new UnknownIdException(SCRIPT_ID)).when(service).cancelScriptExecution(SCRIPT_ID);
    mvc.perform(
          patch("/scripts/" + SCRIPT_ID)
          .content(content)
          .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().is(404))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.title", Matchers.is("Unknown id")))
          .andExpect(jsonPath("$.status", Matchers.is("NOT_FOUND")))
          .andExpect(jsonPath("$.detail", Matchers.is(UnknownIdException.generateMessage(SCRIPT_ID))));
  }

  @Test
  public void shouldPassOnDeletion() throws Exception {
    mvc.perform(delete("/scripts/" + SCRIPT_ID))
          .andExpect(status().is(204));
  }

  @Test
  public void shouldFailOnDeletionOfNotCanceledExec() throws Exception {
    Mockito.doThrow(new DeletionException(SCRIPT_ID)).when(service).deleteScript(SCRIPT_ID);
    mvc.perform(
          delete("/scripts/" + SCRIPT_ID))
          .andExpect(status().is(405))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.title", Matchers.is("Attempt to delete running script")))
          .andExpect(jsonPath("$.status", Matchers.is("METHOD_NOT_ALLOWED")))
          .andExpect(jsonPath("$.detail", Matchers.is(DeletionException.generateMessage(SCRIPT_ID))));
  }

  @Test
  public void shouldFailOnDeletionWithUnknownId() throws Exception {
    Mockito.doThrow(new UnknownIdException(SCRIPT_ID)).when(service).deleteScript(SCRIPT_ID);
    mvc.perform(
          delete("/scripts/" + SCRIPT_ID))
          .andExpect(status().is(404))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.title", Matchers.is("Unknown id")))
          .andExpect(jsonPath("$.status", Matchers.is("NOT_FOUND")))
          .andExpect(jsonPath("$.detail", Matchers.is(UnknownIdException.generateMessage(SCRIPT_ID))));
  }

  @Test
  public void shouldPassOnGettingScriptSource() throws Exception {
    Mockito.when(service.getScriptSource(SCRIPT_ID)).thenReturn(SCRIPT.getBytes());
    MvcResult result = mvc.perform(
          get("/scripts/" + SCRIPT_ID + "/source"))
          .andExpect(status().is(200))
          .andReturn();
    assertEquals(SCRIPT, result.getResponse().getContentAsString());
  }

  @Test
  public void shouldFailOnGettingScriptSourceWithUnknownId() throws Exception {
    Mockito.doThrow(new UnknownIdException(SCRIPT_ID)).when(service).getScriptSource(SCRIPT_ID);
    mvc.perform(
          get("/scripts/" + SCRIPT_ID + "/source"))
          .andExpect(status().is(404))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.title", Matchers.is("Unknown id")))
          .andExpect(jsonPath("$.status", Matchers.is("NOT_FOUND")))
          .andExpect(jsonPath("$.detail", Matchers.is(UnknownIdException.generateMessage(SCRIPT_ID))));
  }

  @Test
  public void shouldPassOnGettingOutput() throws Exception {
    String output = "output";
    Mockito.when(service.getScriptOutput(SCRIPT_ID)).thenReturn(output.getBytes());
    MvcResult result = mvc.perform(get("/scripts/" + SCRIPT_ID + "/output"))
          .andExpect(status().is(200))
          .andExpect(content().contentType(MediaType.TEXT_PLAIN))
          .andReturn();
    assertEquals(output, result.getResponse().getContentAsString());
  }

  @Test
  public void shouldFailOnGettingOutputWithUnknownId() throws Exception {
    Mockito.doThrow(new UnknownIdException(SCRIPT_ID)).when(service).getScriptOutput(SCRIPT_ID);
    mvc.perform(
          get("/scripts/" + SCRIPT_ID + "/output"))
          .andExpect(status().is(404))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.title", Matchers.is("Unknown id")))
          .andExpect(jsonPath("$.status", Matchers.is("NOT_FOUND")))
          .andExpect(jsonPath("$.detail", Matchers.is(UnknownIdException.generateMessage(SCRIPT_ID))));
  }
}
