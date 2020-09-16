package impl.controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromController;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import impl.controllers.doc.CancelExecApiEndPoint;
import impl.controllers.doc.DeleteExecApiEndpoint;
import impl.controllers.doc.ExecuteScriptApiEndpoint;
import impl.controllers.doc.GetExecIdsApiEndpoint;
import impl.controllers.doc.GetExecOutputApiEndpoint;
import impl.controllers.doc.GetExecScriptApiEndpoint;
import impl.controllers.doc.GetExecStatusApiEndpoint;
import impl.controllers.dto.ExceptionStatusResp;
import impl.controllers.dto.ExecReq;
import impl.controllers.dto.StatusResp;
import impl.controllers.dto.CommonStatusResp;
import impl.controllers.dto.ScriptId;
import impl.service.ScriptExecService;
import impl.service.dto.ExecInfo;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;



@RestController
@RequestMapping (
      produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "JS executor")
@AllArgsConstructor
public class ExecutorController {
  private final ScriptExecService service;
  private final ObjectMapper jsonMapper;

  @GetMapping("/")
  @GetExecIdsApiEndpoint
  public ResponseEntity<CollectionModel<?>> getRoot() {
    return ResponseEntity.ok(CollectionModel.of(
            Collections.emptyList(),
            linkTo(methodOn(getClass()).getScripts()).withRel("scripts"),
            linkTo(methodOn(getClass()).getRoot()).withSelfRel()
    ));
  }

  @GetMapping("/scripts")
  @GetExecIdsApiEndpoint
  public ResponseEntity<CollectionModel<ScriptId>> getScripts() {
    Link self = linkTo(methodOn(getClass()).getScripts()).withSelfRel();
    Link blockExec = Link.of(
            fromController(getClass())
                    .path("/scripts")
                    .queryParam("blocking", "true")
                    .toUriString(), "async")
            .withType("POST");
    Link asyncExec = Link.of(
            fromController(getClass())
                    .path("/scripts")
                    .queryParam("blocking", "false")
                    .toUriString(), "blocking")
            .withType("POST");
    List<ScriptId> scripts = getScriptListResp(service.getExecutionIds());
    return ResponseEntity.ok(CollectionModel.of(scripts, self, blockExec, asyncExec));
  }

  @PostMapping(
        path = "/scripts",
        params = "blocking=false",
        consumes = MediaType.APPLICATION_JSON_VALUE
  )
  @ExecuteScriptApiEndpoint
  public ResponseEntity<ScriptId> executeScriptAsync(@RequestBody ExecReq body) {
    String id = service.executeScriptAsync(body.getScript());
    return new ResponseEntity<>(getScriptId(id), HttpStatus.CREATED);
  }

  @PostMapping(
        path = "/scripts",
        params = "blocking=true",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.TEXT_PLAIN_VALUE
  )
  @Hidden
  public ResponseEntity<StreamingResponseBody> executeScriptWithBlocking(@RequestBody ExecReq body) {
    return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
            .body(outputStream -> {
              String id = service.createExec(body.getScript(), outputStream);
              writeId(getScriptId(id), outputStream);
              service.executeScript(id);
          });
  }

  @GetMapping("/scripts/{id}")
  @GetExecStatusApiEndpoint
  public ResponseEntity<StatusResp> getScript(@PathVariable(name = "id") String scriptId) {
    ExecInfo info = service.getExecutionStatus(scriptId);
    StatusResp resp = getExecResp(info);
    resp.add(linkTo(methodOn(ExecutorController.class).getScript(scriptId)).withSelfRel());
    resp.add(linkTo(methodOn(ExecutorController.class).cancelExecution(scriptId)).withRel("cancel").withType("PUT"));
    resp.add(linkTo(methodOn(ExecutorController.class).deleteScript(scriptId)).withRel("delete").withType("DELETE"));
    resp.add(linkTo(methodOn(ExecutorController.class).getScriptText(scriptId)).withRel("text"));
    resp.add(linkTo(methodOn(ExecutorController.class).getScriptOutput(scriptId)).withRel("output"));
    return ResponseEntity.ok(resp);
  }

  @PutMapping("/scripts/{id}")
  @CancelExecApiEndPoint
  public ResponseEntity<ScriptId> cancelExecution(@PathVariable(name = "id") String scriptId) {
    service.cancelExecution(scriptId);
    return ResponseEntity.ok(getScriptId(scriptId));
  }

  @DeleteMapping("/scripts/{id}")
  @DeleteExecApiEndpoint
  public ResponseEntity.HeadersBuilder<?> deleteScript(@PathVariable(name = "id") String scriptId) {
    service.deleteExecution(scriptId);
    return ResponseEntity.noContent();
  }

  @GetMapping(
        path = "/scripts/{id}/text",
        produces = MediaType.TEXT_PLAIN_VALUE
  )
  @GetExecScriptApiEndpoint
  public ResponseEntity<String> getScriptText(@PathVariable(name = "id") String scriptId) {
    return ResponseEntity.ok(service.getExecutionScript(scriptId));
  }

  @GetMapping(
        path = "/scripts/{id}/output",
        produces = MediaType.TEXT_PLAIN_VALUE
  )
  @GetExecOutputApiEndpoint
  public ResponseEntity<String> getScriptOutput(@PathVariable(name = "id") String scriptId) {
    return ResponseEntity.ok(service.getExecutionOutput(scriptId));
  }

  private List<ScriptId> getScriptListResp(List<String> scriptList) {
    return scriptList.stream()
          .map(this::getScriptId)
          .collect(Collectors.toList());
  }

  private StatusResp getExecResp(ExecInfo res) {
    if(res.getMessage().isPresent()) {
      return new ExceptionStatusResp(res.getStatus(), res.getMessage().get());
    } else {
      return new CommonStatusResp(res.getStatus());
    }
  }

  private ScriptId getScriptId(String id) {
    ScriptId scriptId = new ScriptId(id);
    scriptId.add(linkTo(methodOn(ExecutorController.class).getScript(id)).withSelfRel());
    return scriptId;
  }

  private void writeId(ScriptId idDto, OutputStream outputStream) throws IOException {
    JsonGenerator generator = new JsonFactory().createGenerator(outputStream);
    jsonMapper.writeValue(generator, idDto);
    generator.flush();
    outputStream.write('\n');
    outputStream.flush();
  }

}
