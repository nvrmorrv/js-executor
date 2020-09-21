package impl.controllers;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import impl.repositories.entities.ExecStatus;
import impl.service.ScriptExecService;
import impl.service.dto.ExecInfo;
import impl.service.exceptions.DeletionException;
import impl.service.exceptions.SyntaxErrorException;
import impl.repositories.exceptions.UnknownIdException;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import impl.controllers.dto.ExecReq;
import org.springframework.test.web.servlet.MvcResult;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ExecutorController.class)
@AutoConfigureMockMvc
public class ExecutorControllerTest {
  private final String EXEC_ID = "id";
  private final String SCRIPT = "console.log('hello')";
  private final ExecInfo RESULT =
        new ExecInfo(ExecStatus.DONE.name(), Optional.empty());
  private final ExecInfo EX_RESULT =
        new ExecInfo(ExecStatus.DONE_WITH_EXCEPTION.name(), Optional.of(""));
  private final SyntaxErrorException SYN_ERR_EXCEPTION = new SyntaxErrorException("syntax error", "here");

  @Autowired
  private MockMvc mvc;

  @MockBean
  private ScriptExecService service;

  @Autowired
  private ObjectMapper mapper;

  @Test
  public void shouldPassOnGettingRoot() throws Exception {
    mvc.perform(get("/"))
          .andExpect(status().is(200))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/")))
          .andExpect(jsonPath("$._links.scripts.href", Matchers.containsString("/scripts")));
  }

  @Test
  public void shouldPassOnGettingScriptList() throws Exception {
    Mockito.when(service.getExecutionIds()).thenReturn(Collections.singletonList(EXEC_ID));
    mvc.perform(
          get("/scripts"))
          .andExpect(status().is(200))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$._embedded.scripts.[0].id", Matchers.is(EXEC_ID)))
          .andExpect(jsonPath("$._embedded.scripts.[0]._links.self.href", Matchers.containsString(EXEC_ID)))
          .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/scripts")))
          .andExpect(jsonPath("$._links.blocking.href", Matchers.containsString("/scripts?blocking=true")))
          .andExpect(jsonPath("$._links.blocking.type", Matchers.is("POST")))
          .andExpect(jsonPath("$._links.async.href", Matchers.containsString("/scripts?blocking=false")))
          .andExpect(jsonPath("$._links.async.type", Matchers.is("POST")));
  }


  @Test
  public void shouldPassOnAsyncPerformingScript() throws Exception {
    String json = mapper.writeValueAsString(new ExecReq(SCRIPT));
    Mockito.when(service.executeScriptAsync(SCRIPT)).thenReturn(EXEC_ID);
    mvc.perform(
          post("/scripts")
                .content(json)
                .queryParam("blocking", "false")
                .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().is(201))
          .andExpect(header().exists("Location"))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id", Matchers.is(EXEC_ID)))
          .andExpect(jsonPath("$._links.self.href", Matchers.containsString(EXEC_ID)));
  }

  @Test
  public void shouldFailOnAsyncPerformingScriptWithSyntaxError() throws Exception {
    String json = mapper.writeValueAsString(new ExecReq(SCRIPT));
    Mockito.when(service.executeScriptAsync(SCRIPT)).thenThrow(SYN_ERR_EXCEPTION);
    mvc.perform(
          post("/scripts")
                .content(json)
                .queryParam("blocking", "false")
                .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().is(400))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.title", Matchers.is("Syntax error")))
          .andExpect(jsonPath("$.status", Matchers.is("BAD_REQUEST")))
          .andExpect(jsonPath("$.detail", Matchers.is(SYN_ERR_EXCEPTION.getMessage())));
  }

  @Test
  public void shouldPassOnBlockingPerformingScript() throws Exception {
    String json = mapper.writeValueAsString(new ExecReq(SCRIPT));
    Mockito.when(service.createExec(Mockito.eq(SCRIPT), Mockito.any())).thenReturn(EXEC_ID);
    MvcResult result = mvc.perform(
          post("/scripts")
                .content(json)
                .queryParam("blocking", "true")
                .contentType(MediaType.APPLICATION_JSON))
          .andReturn();
    mvc.perform(asyncDispatch(result))
          .andExpect(status().is(200))
          .andExpect(content().contentType(MediaType.TEXT_PLAIN))
          .andExpect(jsonPath("$.id", Matchers.is(EXEC_ID)))
          .andExpect(jsonPath("$._links.self.href", Matchers.containsString(EXEC_ID)));
  }

  //@Test
  public void shouldFailOnBlockingPerformingScriptWithSyntaxError() throws Exception {
    String json = mapper.writeValueAsString(new ExecReq(SCRIPT));
    Mockito.when(service.createExec(Mockito.eq(SCRIPT), Mockito.any())).thenThrow(SYN_ERR_EXCEPTION);
    MvcResult mvcResult =
          mvc.perform(
          post("/scripts")
                .content(json)
                .queryParam("blocking", "true")
                .contentType(MediaType.APPLICATION_JSON))
          .andReturn();
    mvc.perform(asyncDispatch(mvcResult))
          .andExpect(status().is(400))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.title", Matchers.is("Syntax error")))
          .andExpect(jsonPath("$.status", Matchers.is("BAD_REQUEST")))
          .andExpect(jsonPath("$.detail", Matchers.is(SYN_ERR_EXCEPTION.getMessage())));
  }

  @Test
  public void shouldPassOnGettingStatus() throws Exception {
    Mockito.when(service.getExecutionStatus(EXEC_ID)).thenReturn(RESULT);
    mvc.perform(
          get("/scripts/" + EXEC_ID))
          .andExpect(status().is(200))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.status", Matchers.is(RESULT.getStatus())))
          .andExpect(jsonPath("$._links.self.href", Matchers.containsString(EXEC_ID)))
          .andExpect(jsonPath("$._links.text.href", Matchers.containsString(EXEC_ID + "/text")))
          .andExpect(jsonPath("$._links.output.href", Matchers.containsString(EXEC_ID + "/output")))
          .andExpect(jsonPath("$._links.cancel.href", Matchers.containsString(EXEC_ID)))
          .andExpect(jsonPath("$._links.cancel.type", Matchers.is("PUT")))
          .andExpect(jsonPath("$._links.delete.href", Matchers.containsString(EXEC_ID)))
          .andExpect(jsonPath("$._links.delete.type", Matchers.is("DELETE")));
  }

  @Test
  public void shouldPassOnGettingStatusWhenException() throws Exception {
    Mockito.when(service.getExecutionStatus(EXEC_ID)).thenReturn(EX_RESULT);
    mvc.perform(
          get("/scripts/" + EXEC_ID))
          .andExpect(status().is(200))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.status", Matchers.is(EX_RESULT.getStatus())))
          .andExpect(jsonPath("$.message", Matchers.is(EX_RESULT.getMessage().get())))
          .andExpect(jsonPath("$._links.self.href", Matchers.containsString(EXEC_ID)))
          .andExpect(jsonPath("$._links.text.href", Matchers.containsString(EXEC_ID + "/text")))
          .andExpect(jsonPath("$._links.output.href", Matchers.containsString(EXEC_ID + "/output")))
          .andExpect(jsonPath("$._links.cancel.href", Matchers.containsString(EXEC_ID)))
          .andExpect(jsonPath("$._links.cancel.type", Matchers.is("PUT")))
          .andExpect(jsonPath("$._links.delete.href", Matchers.containsString(EXEC_ID)))
          .andExpect(jsonPath("$._links.delete.type", Matchers.is("DELETE")));
  }

  @Test
  public void shouldFailOnGettingStatusWithUnknownId() throws Exception {
    Mockito.when(service.getExecutionStatus(EXEC_ID)).thenThrow(new UnknownIdException(EXEC_ID));
    mvc.perform(
          get("/scripts/" + EXEC_ID))
          .andExpect(status().is(404))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.title", Matchers.is("Unknown id")))
          .andExpect(jsonPath("$.status", Matchers.is("NOT_FOUND")))
          .andExpect(jsonPath("$.detail", Matchers.is(UnknownIdException.generateMessage(EXEC_ID))));
  }

  @Test
  public void shouldPassOnCancellation() throws Exception {
    mvc.perform(
          put("/scripts/" + EXEC_ID))
          .andExpect(status().is(200))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id", Matchers.is(EXEC_ID)))
          .andExpect(jsonPath("$._links.self.href", Matchers.containsString(EXEC_ID)));
  }

  @Test
  public void shouldFailOnCancellationWithUnknownId() throws Exception {
    Mockito.doThrow(new UnknownIdException(EXEC_ID)).when(service).cancelExecution(EXEC_ID);
    mvc.perform(
          put("/scripts/" + EXEC_ID))
          .andExpect(status().is(404))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.title", Matchers.is("Unknown id")))
          .andExpect(jsonPath("$.status", Matchers.is("NOT_FOUND")))
          .andExpect(jsonPath("$.detail", Matchers.is(UnknownIdException.generateMessage(EXEC_ID))));
  }

  @Test
  public void shouldPassOnDeletion() throws Exception {
    mvc.perform(delete("/scripts/" + EXEC_ID))
          .andExpect(status().is(204));
  }

  @Test
  public void shouldFailOnDeletionOfNotCanceledExec() throws Exception {
    Mockito.doThrow(new DeletionException(EXEC_ID)).when(service).deleteExecution(EXEC_ID);
    mvc.perform(
          delete("/scripts/" + EXEC_ID))
          .andExpect(status().is(405))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.title", Matchers.is("Attempt to delete running script")))
          .andExpect(jsonPath("$.status", Matchers.is("METHOD_NOT_ALLOWED")))
          .andExpect(jsonPath("$.detail", Matchers.is(DeletionException.generateMessage(EXEC_ID))));
  }

  @Test
  public void shouldFailOnDeletionWithUnknownId() throws Exception {
    Mockito.doThrow(new UnknownIdException(EXEC_ID)).when(service).deleteExecution(EXEC_ID);
    mvc.perform(
          delete("/scripts/" + EXEC_ID))
          .andExpect(status().is(404))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.title", Matchers.is("Unknown id")))
          .andExpect(jsonPath("$.status", Matchers.is("NOT_FOUND")))
          .andExpect(jsonPath("$.detail", Matchers.is(UnknownIdException.generateMessage(EXEC_ID))));
  }

  @Test
  public void shouldPassOnGettingScriptText() throws Exception {
    Mockito.when(service.getExecutionScript(EXEC_ID)).thenReturn(SCRIPT);
    MvcResult result = mvc.perform(
          get("/scripts/" + EXEC_ID + "/text"))
          .andExpect(status().is(200))
          .andReturn();
    assertEquals(SCRIPT, result.getResponse().getContentAsString());
  }

  @Test
  public void shouldFailOnGettingScriptTextWithUnknownId() throws Exception {
    Mockito.doThrow(new UnknownIdException(EXEC_ID)).when(service).getExecutionScript(EXEC_ID);
    mvc.perform(
          get("/scripts/" + EXEC_ID + "/text"))
          .andExpect(status().is(404))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.title", Matchers.is("Unknown id")))
          .andExpect(jsonPath("$.status", Matchers.is("NOT_FOUND")))
          .andExpect(jsonPath("$.detail", Matchers.is(UnknownIdException.generateMessage(EXEC_ID))));
  }

  @Test
  public void shouldPassOnGettingOutput() throws Exception {
    String output = "output";
    Mockito.when(service.getExecutionOutput(EXEC_ID)).thenReturn(output);
    MvcResult result = mvc.perform(get("/scripts/" + EXEC_ID + "/output"))
          .andExpect(status().is(200))
          .andExpect(content().contentType(MediaType.TEXT_PLAIN))
          .andReturn();
    assertEquals(output, result.getResponse().getContentAsString());
  }

  @Test
  public void shouldFailOnGettingOutputWithUnknownId() throws Exception {
    Mockito.doThrow(new UnknownIdException(EXEC_ID)).when(service).getExecutionOutput(EXEC_ID);
    mvc.perform(
          get("/scripts/" + EXEC_ID + "/output"))
          .andExpect(status().is(404))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.title", Matchers.is("Unknown id")))
          .andExpect(jsonPath("$.status", Matchers.is("NOT_FOUND")))
          .andExpect(jsonPath("$.detail", Matchers.is(UnknownIdException.generateMessage(EXEC_ID))));
  }
}
