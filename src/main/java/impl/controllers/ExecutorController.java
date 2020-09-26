package impl.controllers;

import static impl.controllers.utils.ResponseMapper.getCommonStatusResp;
import static impl.controllers.utils.ResponseMapper.getExceptionStatusResp;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromController;

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
import impl.controllers.utils.CommonStatusRespAssembler;
import impl.service.ScriptExecService;
import impl.shared.ScriptInfo;
import impl.shared.ScriptStatus;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Collections;
import java.util.TimeZone;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@Tag(name = "JS executor")
@AllArgsConstructor
public class ExecutorController {
  private final ScriptExecService service;
  private final CommonStatusRespAssembler respAssembler =
        new CommonStatusRespAssembler(id -> linkTo(methodOn(getClass()).getScript(id)).withSelfRel());

  @GetMapping("/")
  @GetRootApiEndpoint
  public ResponseEntity<CollectionModel<?>> getRoot() {
    return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(CollectionModel.of(
                Collections.emptyList(),
                linkTo(methodOn(getClass()).getScripts(null, null, null)).withRel("scripts"),
                linkTo(methodOn(getClass()).getRoot()).withSelfRel()));
  }

  @GetMapping("/scripts")
  @GetExecListApiEndpoint
  public ResponseEntity<PagedModel<CommonStatusResp>> getScripts(
        @RequestParam(value = "status", required = false, defaultValue = "ANY") String filterStatus,
        @PageableDefault(sort = "createTime") Pageable pageable,
        PagedResourcesAssembler<ScriptInfo> pagedResourcesAssembler) {
    Page<ScriptInfo> page = service.getScriptInfoPage(pageable, filterStatus);
    Link selfLink = linkTo(methodOn(getClass()).getScripts(filterStatus, pageable, pagedResourcesAssembler)).withSelfRel();
    PagedModel<CommonStatusResp> pagedModel = pagedResourcesAssembler.toModel(page, respAssembler, selfLink);
    pagedModel.add(getAsyncExecLink("{id}"), getBlockExecLink("{id}"));
    return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(pagedModel);
  }

  @PutMapping(
        path = "/scripts/{id}",
        params = "blocking=false",
        consumes = MediaType.TEXT_PLAIN_VALUE)
  @ExecuteScriptApiEndpoint
  public ResponseEntity<ScriptId> executeScriptAsync(@PathVariable(name = "id") String scriptId,
                                                     @RequestBody byte[] body,
                                                     TimeZone timeZone) {
    boolean created = service.createScript(scriptId, body, timeZone);
    service.executeScriptAsync(scriptId);
    ScriptId resp = getScriptId(scriptId);
    return (created)
          ? ResponseEntity.created(resp.getRequiredLink("self").toUri())
          .contentType(MediaType.APPLICATION_JSON)
          .body(resp)
          : ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(resp);
  }

  @PutMapping(
        path = "/scripts/{id}",
        consumes = MediaType.TEXT_PLAIN_VALUE)
  @Hidden
  public ResponseEntity<ScriptId> executeScriptAsyncByDefault(@PathVariable(name = "id") String scriptId,
                                                              @RequestBody byte[] body,
                                                              TimeZone timeZone) {
    return executeScriptAsync(scriptId, body, timeZone);
  }

  @PutMapping(
        path = "/scripts/{id}",
        params = "blocking=true",
        consumes = MediaType.TEXT_PLAIN_VALUE)
  @Hidden
  public ResponseEntity<StreamingResponseBody> executeScriptWithBlocking(@PathVariable(name = "id") String scriptId,
                                                                         @RequestBody byte[] body,
                                                                         TimeZone timeZone) {
    boolean created = service.createScript(scriptId, body, timeZone);
    StreamingResponseBody responseBody = outputStream -> service.executeScript(scriptId, outputStream);
    return (created)
          ? ResponseEntity.created(linkTo(methodOn(getClass()).getScript(scriptId)).withRel("self").toUri())
          .contentType(MediaType.TEXT_PLAIN)
          .body(responseBody)
          : ResponseEntity.ok()
          .contentType(MediaType.TEXT_PLAIN)
          .body(responseBody);
  }

  @GetMapping("/scripts/{id}")
  @GetExecStatusApiEndpoint
  public ResponseEntity<StatusResp> getScript(@PathVariable(name = "id") String scriptId) {
    ScriptInfo info = service.getScriptInfo(scriptId);
    StatusResp resp = getStatusResp(info);
    resp.add(linkTo(methodOn(ExecutorController.class).getScript(scriptId)).withSelfRel());
    resp.add(linkTo(methodOn(ExecutorController.class).cancelExecution(null, scriptId)).withRel("cancel").withType("PATCH"));
    resp.add(linkTo(methodOn(ExecutorController.class).deleteScript(scriptId)).withRel("delete").withType("DELETE"));
    resp.add(linkTo(methodOn(ExecutorController.class).getScriptSource(scriptId)).withRel("source"));
    resp.add(linkTo(methodOn(ExecutorController.class).getScriptOutput(scriptId)).withRel("output"));
    resp.add(getAsyncExecLink(scriptId));
    resp.add(getBlockExecLink(scriptId));
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resp);
  }

  @PatchMapping("/scripts/{id}")
  @CancelExecApiEndpoint
  public ResponseEntity<ScriptId> cancelExecution(@RequestBody CancelReq req,
                                                  @PathVariable(name = "id") String scriptId) {
    if(!req.getStatus().equals(ScriptStatus.CANCELLED.name())){
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

  @GetMapping("/scripts/{id}/source")
  @GetExecScriptApiEndpoint
  public ResponseEntity<byte[]> getScriptSource(@PathVariable(name = "id") String scriptId) {
    return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(service.getScriptSource(scriptId));
  }

  @GetMapping("/scripts/{id}/output")
  @GetExecOutputApiEndpoint
  public ResponseEntity<byte[]> getScriptOutput(@PathVariable(name = "id") String scriptId) {
    return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(service.getScriptOutput(scriptId));
  }

  private Link getBlockExecLink(String id) {
    return Link.of(fromController(getClass())
          .path("/scripts/" + id)
          .queryParam("blocking", "true")
          .build().toString(), "blocking")
          .withType("PUT");
  }

  private Link getAsyncExecLink(String id) {
    return Link.of(fromController(getClass()).pathSegment()
          .path("/scripts/" + id)
          .queryParam("blocking", "false")
          .build().toString(), "async")
          .withType("PUT");
  }

  private StatusResp getStatusResp(ScriptInfo info) {
    if (info.getStatus() != ScriptStatus.DONE_WITH_EXCEPTION) {
      return getCommonStatusResp(info);
    } else {
      return getExceptionStatusResp(info);
    }
  }

  private ScriptId getScriptId(String id) {
    ScriptId scriptId = new ScriptId(id);
    scriptId.add(linkTo(methodOn(getClass()).getScript(id)).withRel("self"));
    return scriptId;
  }
}
