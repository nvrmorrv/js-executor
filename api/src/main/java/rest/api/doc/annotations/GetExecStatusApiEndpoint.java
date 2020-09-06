package rest.api.doc.annotations;

import static java.lang.annotation.ElementType.METHOD;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import rest.api.dto.ErrorResp;
import rest.api.dto.ExceptionResp;
import rest.api.dto.ExecStatusResp;

@Operation(
      summary = "Get execution status",
      tags = { "script" },
      parameters = {@Parameter(
            name = "id",
            in = ParameterIn.PATH,
            required = true
      )})
@ApiResponses(value = {
      @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = {
                  @Content(
                        mediaType = "application/json",
                        schema = @Schema(anyOf = {ExceptionResp.class, ExecStatusResp.class}))
            }),
      @ApiResponse(
            responseCode = "404",
            description = "Error: unknown id",
            content = {
                  @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResp.class))
            }),
      @ApiResponse(
            responseCode = "500",
            description = "Error: server error",
            content = {
                  @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResp.class))
            })
})
@Target({METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GetExecStatusApiEndpoint {
}
