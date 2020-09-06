package impl.controllers;

import impl.service.ScriptExecService;
import impl.service.dto.ExecInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rest.api.ExecutorRestApi;
import rest.api.dto.ExceptionResp;
import rest.api.dto.ExecReq;
import rest.api.dto.ExecResp;
import rest.api.dto.ExecStatusResp;
import rest.api.dto.ScriptId;
import rest.api.dto.ScriptListResp;


@RestController
@RequestMapping (
      path = "/executor/js",
      produces = MediaType.APPLICATION_JSON_VALUE
)
@ResponseBody
@Tag(name = "JS executor")
public class ExecutorController implements ExecutorRestApi {
  private final ScriptExecService service;
  private final long execTimeout;

  public ExecutorController(ScriptExecService service,
                            @Value("${executor.blocking-timeout}") Long execTimeout) {
    this.service = service;
    this.execTimeout = execTimeout;
  }

  @PostMapping(
        path = "/script",
        params = "blocking=false",
        consumes = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseStatus(HttpStatus.CREATED)
  public ScriptId executeScriptAsync(@RequestBody ExecReq body) {
    return new ScriptId(service.executeScriptAsync(body.getScript()));
  }

  @PostMapping(
        path = "/script",
        params = "blocking=true",
        consumes = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseStatus(HttpStatus.OK)
  public ExecResp executeScriptWithBlocking(@RequestBody ExecReq body) {
    ExecInfo res = service.executeScript(body.getScript(), execTimeout, TimeUnit.MINUTES);
    return getExecResp(res);
  }

  @GetMapping("/script/{id}")
  @ResponseStatus(HttpStatus.OK)
  public ExecResp getExecutionStatus(@PathVariable(name = "id") String scriptId) {
    ExecInfo info = service.getExecutionStatus(scriptId);
    return getExecResp(info);
  }

  @PutMapping("/script/{id}")
  @ResponseStatus(HttpStatus.OK)
  public void cancelExecution(@PathVariable(name = "id") String scriptId) {
    service.cancelExecution(scriptId);
  }

  @DeleteMapping("/script/{id}")
  @ResponseStatus(HttpStatus.OK)
  public void deleteExecution(@PathVariable(name = "id") String scriptId) {
    service.deleteExecution(scriptId);
  }

  @GetMapping("/script-list")
  @ResponseStatus(HttpStatus.OK)
  public ScriptListResp getAllExecutions() {
    return getScriptListResp(service.getExecutionIds());
  }

  private ScriptListResp getScriptListResp(List<String> scriptList) {
    return new ScriptListResp(scriptList.stream()
          .map(ScriptId::new)
          .collect(Collectors.toList()));
  }

  private ExecResp getExecResp(ExecInfo res) {
    if(res.getMessage().isPresent()) {
      return new ExceptionResp(res.getStatus(), res.getMessage().get(), res.getOutput());
    } else {
      return new ExecStatusResp(res.getStatus(), res.getOutput());
    }
  }
}
