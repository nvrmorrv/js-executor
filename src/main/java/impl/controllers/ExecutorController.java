package impl.controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

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
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.hateoas.CollectionModel;
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
      path = "/executor/js",
      produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "JS executor")
@AllArgsConstructor
public class ExecutorController {
  private final ScriptExecService service;
  private final ObjectMapper jsonMapper;

  @PostMapping(
        path = "/script",
        params = "blocking=false",
        consumes = MediaType.APPLICATION_JSON_VALUE
  )
  @ExecuteScriptApiEndpoint
  public ResponseEntity<ScriptId> executeScriptAsync(@RequestBody ExecReq body) {
    String id = service.executeScriptAsync(body.getScript());
    return new ResponseEntity<>(getScriptIdWithLinks(id), HttpStatus.CREATED);
  }

  @PostMapping(
        path = "/script",
        params = "blocking=true",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.TEXT_PLAIN_VALUE
  )
  @Hidden
  public ResponseEntity<StreamingResponseBody> executeScriptWithBlocking(@RequestBody ExecReq body) {
    return ResponseEntity.ok(
          outputStream -> {
            String id = service.createExec(body.getScript(), outputStream);
            writeId(getScriptIdWithLinks(id), outputStream);
            service.executeScript(id);
          });
  }

  @GetMapping("/script/{id}")
  @GetExecStatusApiEndpoint
  public ResponseEntity<StatusResp> getExecutionStatus(@PathVariable(name = "id") String scriptId) {
    ExecInfo info = service.getExecutionStatus(scriptId);
    StatusResp resp = getExecResp(info);
    resp.add(linkTo(methodOn(ExecutorController.class).getExecutionScript(scriptId)).withRel("code"));
    resp.add(linkTo(methodOn(ExecutorController.class).getExecutionOutput(scriptId)).withRel("output"));
    return ResponseEntity.ok(resp);
  }

  @GetMapping(
        path = "/script/{id}/code",
        produces = MediaType.TEXT_PLAIN_VALUE
  )
  @GetExecScriptApiEndpoint
  public ResponseEntity<String> getExecutionScript(@PathVariable(name = "id") String scriptId) {
    return ResponseEntity.ok(service.getExecutionScript(scriptId));
  }

  @GetMapping(
        path = "/script/{id}/output",
        produces = MediaType.TEXT_PLAIN_VALUE
  )
  @GetExecOutputApiEndpoint
  public ResponseEntity<String> getExecutionOutput(@PathVariable(name = "id") String scriptId) {
    return ResponseEntity.ok(service.getExecutionOutput(scriptId));
  }

  @PutMapping("/script/{id}")
  @CancelExecApiEndPoint
  public ResponseEntity<Void> cancelExecution(@PathVariable(name = "id") String scriptId) {
    service.cancelExecution(scriptId);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @DeleteMapping("/script/{id}")
  @DeleteExecApiEndpoint
  public ResponseEntity<Void> deleteExecution(@PathVariable(name = "id") String scriptId) {
    service.deleteExecution(scriptId);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @GetMapping("/script-list")
  @GetExecIdsApiEndpoint
  public ResponseEntity<CollectionModel<ScriptId>> getAllExecutions() {
    return ResponseEntity.ok(CollectionModel.of(getScriptListResp(service.getExecutionIds())));
  }

  private List<ScriptId> getScriptListResp(List<String> scriptList) {
    return scriptList.stream()
          .map(this::getScriptIdWithLinks)
          .collect(Collectors.toList());
  }

  private StatusResp getExecResp(ExecInfo res) {
    if(res.getMessage().isPresent()) {
      return new ExceptionStatusResp(res.getStatus(), res.getMessage().get());
    } else {
      return new CommonStatusResp(res.getStatus());
    }
  }

  private ScriptId getScriptIdWithLinks(String id) {
    ScriptId scriptId = new ScriptId(id);
    scriptId.add(linkTo(methodOn(ExecutorController.class).getExecutionStatus(id)).withRel("status").withType("GET"));
    scriptId.add(linkTo(methodOn(ExecutorController.class).cancelExecution(id)).withRel("cancel").withType("PUT"));
    scriptId.add(linkTo(methodOn(ExecutorController.class).getExecutionStatus(id)).withRel("delete").withType("DELETE"));
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
