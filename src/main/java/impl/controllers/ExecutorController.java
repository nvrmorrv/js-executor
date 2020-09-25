package impl.controllers;

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
import impl.service.ScriptExecService;
import impl.service.dto.ScriptInfo;
import impl.service.dto.SortParams;
import impl.shared.ExecStatus;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
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
  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS;dd-MM-uuuu;O");

  @GetMapping("/")
  @GetRootApiEndpoint
  public ResponseEntity<CollectionModel<?>> getRoot() {
    return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(CollectionModel.of(
                Collections.emptyList(),
                linkTo(methodOn(getClass()).getScripts(null, null)).withRel("scripts"),
                linkTo(methodOn(getClass()).getRoot()).withSelfRel()));
  }

  @GetMapping("/scripts")
  @GetExecListApiEndpoint
  public ResponseEntity<PagedModel<EntityModel<CommonStatusResp>>> getScripts(
        Pageable pageable,
        PagedResourcesAssembler<CommonStatusResp> assembler/*,
        @RequestParam(name = "sort-field", required = false, defaultValue = "create-time") String sortField,
        @RequestParam(name = "sort-order", required = false, defaultValue = "asc") String sortOrder,
        @RequestParam(name = "status", required = false, defaultValue = "any") String status*/) {
    //SortParams sortParams = new SortParams(sortField, sortOrder, status);
    List<CommonStatusResp> scripts = getScriptListResp(service.getScripts(null));
    Page<CommonStatusResp> page = new PageImpl<>(scripts, pageable, scripts.size());
    Link selfLink = linkTo(methodOn(getClass()).getScripts(pageable, assembler)).withSelfRel();
    PagedModel<EntityModel<CommonStatusResp>> pagedModel = assembler.toModel(page, selfLink);
    pagedModel.add(getAsyncExecLink("{id}"), getBlockExecLink("{id}"));
    return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(pagedModel);
//          .body(CollectionModel.of(scripts,
//                selfLink, getAsyncExecLink("{id}"), getBlockExecLink("{id}")));
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
    StatusResp resp = getExecResp(info);
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
    if(!req.getStatus().equals(ExecStatus.CANCELLED.name())){
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

  private List<CommonStatusResp> getScriptListResp(List<ScriptInfo> scriptList) {
    return scriptList.stream()
          .map(this::getCommonStatusResp)
          .peek(resp -> resp.add(linkTo(methodOn(getClass()).getScript(resp.getId())).withRel("self")))
          .collect(Collectors.toList());
  }

  private StatusResp getExecResp(ScriptInfo info) {
    if(info.getStatus() != ExecStatus.DONE_WITH_EXCEPTION) {
      return getCommonStatusResp(info);
    } else {
      return new ExceptionStatusResp(
            info.getId(),
            info.getStatus().name(),
            info.getCreateTime().format(formatter),
            getString(info.getStartTime()),
            getString(info.getFinishTime()),
            info.getMessage().orElse(""),
            info.getStackTrace().orElse(Collections.emptyList()));
    }
  }

  private CommonStatusResp getCommonStatusResp(ScriptInfo info) {
    return new CommonStatusResp(
          info.getId(),
          info.getStatus().name(),
          info.getCreateTime().format(formatter),
          getString(info.getStartTime()),
          getString(info.getFinishTime()));
  }

  private ScriptId getScriptId(String id) {
    ScriptId scriptId = new ScriptId(id);
    scriptId.add(linkTo(methodOn(getClass()).getScript(id)).withRel("self"));
    return scriptId;
  }

  private String getString(Optional<ZonedDateTime> dateTime) {
    return dateTime.map(zonedDateTime -> zonedDateTime.format(formatter)).orElse("");
  }
}
