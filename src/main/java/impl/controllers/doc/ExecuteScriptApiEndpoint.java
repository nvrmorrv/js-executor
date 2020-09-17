package impl.controllers.doc;

import static java.lang.annotation.ElementType.METHOD;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import impl.controllers.doc.resp.InternalSerErrResp;
import impl.controllers.dto.ExecReq;
import impl.controllers.dto.ScriptId;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.http.MediaType;

@Operation(
      summary = "Execute script",
      description =
            "There are async and blocking requests. " +
                  "The first one returns the execution id, " +
                  "the second also returns id along with execution output in a streaming way.",
      tags = { "script" },
      requestBody = @RequestBody(
            description = "script for executing",
            content = @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ExecReq.class)
            ),
            required = true
      ),
      parameters = {
            @Parameter(
                  name = "blocking",
                  description = "specifies execution type",
                  in = ParameterIn.QUERY,
                  examples = {
                        @ExampleObject(name = "async request", value = "false"),
                        @ExampleObject(name = "blocking request", value = "true")},
                  required = true
      )})
@ApiResponse(
      responseCode = "201",
      description = "Result of async request",
      content = {
            @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ScriptId.class))
      })
@ApiResponse(
      responseCode = "200",
      description = "Result of blocking request",
      content = {
            @Content(
                   mediaType = "*/*",
                   schema = @Schema(implementation = ScriptId.class))
            })
@ApiResponse(
      responseCode = "400",
      description = "Syntax error",
      content = {
            @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = Problem.class))
            })
@InternalSerErrResp
@Target({METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExecuteScriptApiEndpoint {
}
