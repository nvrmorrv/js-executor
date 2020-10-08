package impl.controllers;

import static impl.controllers.utils.ResponseMapper.getCommonStatusResp;
import static impl.controllers.utils.ResponseMapper.getExceptionStatusResp;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromController;

import impl.controllers.dto.*;
import impl.controllers.exceptions.CancellationException;
import impl.controllers.utils.CommonStatusRespAssembler;
import impl.security.ScriptRecourseAccess;
import impl.service.ScriptExecService;
import impl.shared.ScriptInfo;
import impl.shared.ScriptStatus;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@AllArgsConstructor
public class ExecutorController {
  private final ScriptExecService service;
  private final CommonStatusRespAssembler commonStatusRespAssembler =
        new CommonStatusRespAssembler(id -> linkTo(methodOn(getClass()).getScript(id)).withSelfRel());

  @GetMapping("/")
  public ResponseEntity<CollectionModel<?>> getRoot() {
    return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(CollectionModel.of(
                Collections.emptyList(),
                linkTo(methodOn(getClass()).getScripts(null, null,  null)).withRel("scripts"),
                linkTo(methodOn(getClass()).getRoot()).withSelfRel()));
  }

  @GetMapping("/scripts")
  public ResponseEntity<PagedModel<CommonScriptResp>> getScripts(
        @RequestParam(value = "status", required = false, defaultValue = "ANY") String filterStatus,
        @PageableDefault(sort = "createTime") Pageable pageable,
        PagedResourcesAssembler<ScriptInfo> pagedResourcesAssembler) {
    Page<ScriptInfo> page = service.getScriptInfoPage(pageable, filterStatus);
    Link selfLink = Link.of(fromController(getClass())
          .path("/scripts")
          .queryParam("status", filterStatus)
          .queryParam("page", pageable.getPageNumber())
          .queryParam("size", pageable.getPageSize())
          .queryParam("sort", getPageableSortValues(pageable.getSort()))
          .build().toString()).withSelfRel();
    PagedModel<CommonScriptResp> pagedModel = pagedResourcesAssembler.toModel(page, commonStatusRespAssembler, selfLink);
    pagedModel.add(getAsyncExecLink("{id}"), getBlockExecLink("{id}"));
    return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(pagedModel);
  }

  @PutMapping(
        path = "/scripts/{id}",
        params = "blocking=false",
        consumes = MediaType.TEXT_PLAIN_VALUE)
  @ScriptRecourseAccess
  public ResponseEntity<ScriptId> executeScriptAsync(@PathVariable(name = "id") String scriptId,
                                                     @RequestBody byte[] source,
                                                     TimeZone timeZone,
                                                     Principal principal) {
    boolean created = service.createScript(scriptId, getOwnerEmail(principal), source, timeZone);
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
  @ScriptRecourseAccess
  public ResponseEntity<ScriptId> executeScriptAsyncByDefault(@PathVariable(name = "id") String scriptId,
                                                              @RequestBody byte[] source,
                                                              TimeZone timeZone,
                                                              Principal principal) {
    return executeScriptAsync(scriptId, source, timeZone, principal);
  }

  @PutMapping(
        path = "/scripts/{id}",
        params = "blocking=true",
        consumes = MediaType.TEXT_PLAIN_VALUE)
  @ScriptRecourseAccess
  public ResponseEntity<StreamingResponseBody> executeScriptWithBlocking(@PathVariable(name = "id") String scriptId,
                                                                         @RequestBody byte[] source,
                                                                         TimeZone timeZone,
                                                                         Principal principal) {
    boolean created = service.createScript(scriptId, getOwnerEmail(principal), source, timeZone);
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
  @ScriptRecourseAccess
  public ResponseEntity<ScriptResp> getScript(@PathVariable(name = "id") String scriptId) {
    ScriptInfo info = service.getScriptInfo(scriptId);
    ScriptResp resp = getStatusResp(info);
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
  @ScriptRecourseAccess
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
  @ScriptRecourseAccess
  public ResponseEntity<?> deleteScript(@PathVariable(name = "id") String scriptId) {
    service.deleteScript(scriptId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/scripts/{id}/source")
  @ScriptRecourseAccess
  public ResponseEntity<byte[]> getScriptSource(@PathVariable(name = "id") String scriptId) {
    return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(service.getScriptSource(scriptId));
  }

  @GetMapping("/scripts/{id}/output")
  @ScriptRecourseAccess
  public ResponseEntity<byte[]> getScriptOutput(@PathVariable(name = "id") String scriptId) {
    return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(service.getScriptOutput(scriptId));
  }

  private String getOwnerEmail(Principal principal) {
    return ((OAuth2AuthenticationToken)principal).getPrincipal().getAttribute("email");
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

  private List<String> getPageableSortValues(Sort sort) {
    return sort.stream()
          .map(order -> (order.isAscending())
                ? order.getProperty() + ",asc"
                : order.getProperty() + ",desc")
          .collect(Collectors.toList());
  }

  private ScriptResp getStatusResp(ScriptInfo info) {
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
