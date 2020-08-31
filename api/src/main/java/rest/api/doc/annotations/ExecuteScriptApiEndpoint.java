package rest.api.doc.annotations;

import static java.lang.annotation.ElementType.METHOD;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import rest.api.dto.BlockingExecResp;
import rest.api.dto.ErrorResp;
import rest.api.dto.ExecReq;
import rest.api.dto.ScriptId;

@Operation(
      summary = "Execute script",
      description = "There are async and blocking requests. " +
            "The first one returns execution id, the second -- execution result info.",
      tags = { "script" },
      requestBody = @RequestBody(
            description = "script for executing",
            content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ExecReq.class)
            ),
            required = true
      ),
      parameters = {@Parameter(
            name = "blocking",
            description = "specifies execution type",
            in = ParameterIn.QUERY,
            examples = {
                  @ExampleObject(name = "async request", value = "false"),
                  @ExampleObject(name = "blocking request", value = "true")},
            required = true
      )})
@ApiResponses(value = {
      @ApiResponse(
            responseCode = "201",
            description = "Result of async request",
            content = {
                  @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ScriptId.class))
            }),
      @ApiResponse
            (responseCode = "200",
            description = "Result of blocking request",
            content = {
                  @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = BlockingExecResp.class))
            }),
      @ApiResponse(responseCode = "403",
            description = "Error: blocking time is over",
            content = {
                  @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResp.class))
      }),
      @ApiResponse(responseCode = "500",
            description = "Error: server error",
            content = {
                  @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResp.class))
      })
})
@Target({METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExecuteScriptApiEndpoint {
}
