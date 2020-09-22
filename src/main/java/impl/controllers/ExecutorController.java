package impl.controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromController;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import impl.controllers.doc.CancelExecApiEndpoint;
import impl.controllers.doc.DeleteExecApiEndpoint;
import impl.controllers.doc.ExecuteScriptApiEndpoint;
import impl.controllers.doc.GetExecListApiEndpoint;
import impl.controllers.doc.GetExecOutputApiEndpoint;
import impl.controllers.doc.GetExecScriptApiEndpoint;
import impl.controllers.doc.GetExecStatusApiEndpoint;
import impl.controllers.doc.GetRootApiEndpoint;
import impl.controllers.dto.*;
import impl.controllers.exceptions.CancellationException;
import impl.repositories.entities.ExecStatus;
import impl.service.ScriptExecService;
import impl.service.dto.ScriptInfo;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@Tag(name = "JS executor")
@AllArgsConstructor
public class ExecutorController {
  private final ScriptExecService service;

  @GetMapping("/")
  @GetRootApiEndpoint
  public ResponseEntity<CollectionModel<?>> getRoot() {
    return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(CollectionModel.of(
                Collections.emptyList(),
                linkTo(methodOn(getClass()).getScripts()).withRel("scripts"),
                linkTo(methodOn(getClass()).getRoot()).withSelfRel()));
  }

  @GetMapping("/scripts")
  @GetExecListApiEndpoint
  public ResponseEntity<CollectionModel<CommonStatusResp>> getScripts() {
    Link self = linkTo(methodOn(getClass()).getScripts()).withSelfRel();
    Link blockExec = Link.of(fromController(getClass())
                .path("/scripts")
                .queryParam("blocking", "false")
                .toUriString(), "async")
          .withType("POST");
    Link asyncExec = Link.of(fromController(getClass())
                .path("/scripts")
                .queryParam("blocking", "true")
                .toUriString(), "blocking")
          .withType("POST");
    List<CommonStatusResp> scripts = getScriptListResp(service.getScripts());
    return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(CollectionModel.of(scripts, self, blockExec, asyncExec));
  }

  @PutMapping(
        path = "/scripts/{id}",
        params = "blocking=false",
        consumes = MediaType.TEXT_PLAIN_VALUE)
  @ExecuteScriptApiEndpoint
  public ResponseEntity<ScriptId> executeScriptAsync(@PathVariable(name = "id") String scriptId,
                                                     @RequestBody byte[] body,
                                                     TimeZone timeZone) {
    boolean exist = service.createScript(scriptId, body, timeZone);
    service.executeScriptAsync(scriptId);
    ScriptId resp = getScriptId(scriptId);
    return (exist)
          ? ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(resp)
          : ResponseEntity.created(resp.getRequiredLink("self").toUri())
          .contentType(MediaType.APPLICATION_JSON)
          .body(resp);
  }

  @PutMapping(
        path = "/scripts/{id}",
        params = "blocking=true",
        consumes = MediaType.TEXT_PLAIN_VALUE)
  @Hidden
  public ResponseEntity<StreamingResponseBody> executeScriptWithBlocking(@PathVariable(name = "id") String scriptId,
                                                                         @RequestBody byte[] body,
                                                                         TimeZone timeZone) {
    boolean exist = service.createScript(scriptId, body, timeZone);
    StreamingResponseBody responseBody = outputStream -> service.executeScript(scriptId, outputStream);
    return (exist)
          ? ResponseEntity.ok()
          .contentType(MediaType.TEXT_PLAIN)
          .body(responseBody)
          : ResponseEntity.created(linkTo(methodOn(getClass()).getScript(scriptId)).withRel("self").toUri())
          .contentType(MediaType.TEXT_PLAIN)
          .body(responseBody);
  }

  @GetMapping("/scripts/{id}")
  @GetExecStatusApiEndpoint
  public ResponseEntity<StatusResp> getScript(@PathVariable(name = "id") String scriptId) {
    ScriptInfo info = service.getScriptInfo(scriptId);
    StatusResp resp = getExecResp(info);
    resp.add(linkTo(methodOn(ExecutorController.class).getScript(scriptId)).withSelfRel());
    resp.add(linkTo(methodOn(ExecutorController.class).cancelExecution(null, scriptId)).withRel("cancel").withType("PUT"));
    resp.add(linkTo(methodOn(ExecutorController.class).deleteScript(scriptId)).withRel("delete").withType("DELETE"));
    resp.add(linkTo(methodOn(ExecutorController.class).getScriptText(scriptId)).withRel("text"));
    resp.add(linkTo(methodOn(ExecutorController.class).getScriptOutput(scriptId)).withRel("output"));
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resp);
  }

  @PatchMapping("/scripts/{id}")
  @CancelExecApiEndpoint
  public ResponseEntity<ScriptId> cancelExecution(@RequestBody CancelReq req,
                                                  @PathVariable(name = "id") String scriptId) {
    if(!req.getStatus().equals("CANCELLED")){
      throw new CancellationException();
    }
    service.cancelScriptExecution(scriptId);
    return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(getScriptId(scriptId));
  }

  @DeleteMapping("/scripts/{id}")
  @DeleteExecApiEndpoint
  public ResponseEntity<?> deleteScript(@PathVariable(name = "id") String scriptId) {
    service.deleteScript(scriptId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/scripts/{id}/text")
  @GetExecScriptApiEndpoint
  public ResponseEntity<byte[]> getScriptText(@PathVariable(name = "id") String scriptId) {
    return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(service.getScriptText(scriptId));
  }

  @GetMapping("/scripts/{id}/err-output")
  @GetExecOutputApiEndpoint
  public ResponseEntity<byte[]> getScriptErrOutput(@PathVariable(name = "id") String scriptId) {
    return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(service.getScriptErrOutput(scriptId));
  }

  @GetMapping("/scripts/{id}/output")
  @GetExecOutputApiEndpoint
  public ResponseEntity<byte[]> getScriptOutput(@PathVariable(name = "id") String scriptId) {
    return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(service.getScriptOutput(scriptId));
  }

  private List<CommonStatusResp> getScriptListResp(List<ScriptInfo> scriptList) {
    return scriptList.stream()
          .map(this::getCommonStatusResp)
          .collect(Collectors.toList());
  }

  private StatusResp getExecResp(ScriptInfo info) {
    if(info.getMessage().isEmpty()) {
      return getCommonStatusResp(info);
    } else {
      return new ExceptionStatusResp(info.getId(), info.getStatus(), info.getScheduledTime(),
            info.getStartTime(), info.getScheduledTime(), info.getMessage(), info.getStackTrace());
    }
  }

  private CommonStatusResp getCommonStatusResp(ScriptInfo info) {
    return new CommonStatusResp(info.getId(), info.getStatus(), info.getScheduledTime(),
          info.getStartTime(), info.getScheduledTime());
  }

  private ScriptId getScriptId(String id) {
    ScriptId scriptId = new ScriptId(id);
    scriptId.add(linkTo(methodOn(getClass()).getScript(id)).withRel("self"));
    return scriptId;
  }
}
