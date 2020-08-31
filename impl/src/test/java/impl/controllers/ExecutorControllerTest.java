package impl.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import impl.service.ExecStatus;
import impl.service.ScriptExecService;
import impl.service.dto.ExecInfo;
import impl.service.exceptions.DeletionException;
import impl.service.exceptions.ExecTimeOutException;
import impl.service.exceptions.UnknownIdException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
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
import rest.api.dto.ExecReq;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ExecutorController.class)
@AutoConfigureMockMvc
public class ExecutorControllerTest {
  private final String EXEC_ID = "id";
  private final String SCRIPT = "console.log('hello')";
  private final ExecInfo RESULT =
        new ExecInfo(ExecStatus.DONE.name(), "hello");

  @Autowired
  private MockMvc mvc;

  @MockBean
  private ScriptExecService service;

  @Autowired
  private ObjectMapper mapper;


  @Test
  public void shouldPassOnPerformingScriptAsync() throws Exception {
    String json = mapper.writeValueAsString(new ExecReq(SCRIPT));
    Mockito.when(service.executeScriptAsync(SCRIPT)).thenReturn(EXEC_ID);
    mvc.perform(
          post("/executor/js/script")
                .content(json)
                .queryParam("blocking", "false")
                .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().is(201))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.id", Matchers.is(EXEC_ID)));
  }

  @Test
  public void shouldPassOnPerformingScriptWithBlocking() throws Exception {
    String json = mapper.writeValueAsString(new ExecReq(SCRIPT));
    Mockito.when(
          service.executeScript(
                Mockito.eq(SCRIPT),
                Mockito.anyLong(),
                Mockito.any()))
          .thenReturn(RESULT);
    mvc.perform(
          post("/executor/js/script")
                .content(json)
                .queryParam("blocking", "true")
                .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().is(200))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.resultStatus", Matchers.is(RESULT.getStatus())))
          .andExpect(jsonPath("$.output", Matchers.is(RESULT.getOutput())));
  }

  @Test
  public void shouldFailOnPerformingScriptWithBlockingWhenTimeout() throws Exception {
    String json = mapper.writeValueAsString(new ExecReq(SCRIPT));
    long timeout = 5;
    TimeUnit timeUnit = TimeUnit.MINUTES;
    Mockito.when(
          service.executeScript(
                Mockito.eq(SCRIPT),
                Mockito.anyLong(),
                Mockito.any()))
          .thenThrow(new ExecTimeOutException(timeout, timeUnit));
    mvc.perform(
          post("/executor/js/script")
                .content(json)
                .queryParam("blocking", "true")
                .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().is(403))
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.error",
                Matchers.is(ExecTimeOutException.generateMessage(timeout, timeUnit))));
  }

  @Test
  public void shouldPassOnGettingStatus() throws Exception {
    ExecInfo status = new ExecInfo(ExecStatus.QUEUE.name(), "");
    Mockito.when(service.getExecutionStatus(EXEC_ID)).thenReturn(status);
    mvc.perform(
          get("/executor/js/script/" + EXEC_ID))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.status", Matchers.is(status.getStatus())))
          .andExpect(jsonPath("$.output", Matchers.is(status.getOutput())));
  }

  @Test
  public void shouldFailOnGettingStatusWithUnknownId() throws Exception {
    Mockito.when(service.getExecutionStatus(EXEC_ID))
          .thenThrow(new UnknownIdException(EXEC_ID));
    mvc.perform(
          get("/executor/js/script/" + EXEC_ID))
          .andExpect(status().isNotFound())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.error",
                Matchers.is(UnknownIdException.generateMessage(EXEC_ID))));
  }

  @Test
  public void shouldPassOnCancellation() throws Exception {
    mvc.perform(
          put("/executor/js/script/" + EXEC_ID))
          .andExpect(status().isOk());
  }

  @Test
  public void shouldFailOnCancellationWithUnknownId() throws Exception {
    Mockito.doThrow(new UnknownIdException(EXEC_ID))
          .when(service).cancelExecution(EXEC_ID);
    mvc.perform(
          put("/executor/js/script/" + EXEC_ID))
          .andExpect(status().isNotFound())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.error",
                Matchers.is(UnknownIdException.generateMessage(EXEC_ID))));
  }

  @Test
  public void shouldPassOnDeletion() throws Exception {
    mvc.perform(
          delete("/executor/js/script/" + EXEC_ID))
          .andExpect(status().isOk());
  }

  @Test
  public void shouldFailOnDeletionOfNotCanceledExec() throws Exception {
    Mockito.doThrow(new DeletionException(EXEC_ID))
          .when(service).deleteExecution(EXEC_ID);
    mvc.perform(
          delete("/executor/js/script/" + EXEC_ID))
          .andExpect(status().isMethodNotAllowed())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.error",
                Matchers.is(DeletionException.generateMessage(EXEC_ID))));
  }

  @Test
  public void shouldFailOnDeletionWithUnknownId() throws Exception {
    Mockito.doThrow(new UnknownIdException(EXEC_ID))
          .when(service).deleteExecution(EXEC_ID);
    mvc.perform(
          delete("/executor/js/script/" + EXEC_ID))
          .andExpect(status().isNotFound())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.error",
                Matchers.is(UnknownIdException.generateMessage(EXEC_ID))));
  }

  @Test
  public void shouldPassOnGettingExecList() throws Exception {
    Mockito.when(service.getAllExecutionIds())
          .thenReturn(Collections.singletonList(EXEC_ID));
    mvc.perform(
          get("/executor/js/script-list"))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.scripts.[0].id",
                Matchers.is(EXEC_ID)));
  }

  @Test
  public void shouldPassOnGettingFinishedExecList() throws Exception {
    Mockito.when(service.getFinishedExecutionIds())
          .thenReturn(Collections.singletonList(EXEC_ID));
    mvc.perform(
          get("/executor/js/script-list/finished"))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.scripts.[0].id",
                Matchers.is(EXEC_ID)));
  }

}
