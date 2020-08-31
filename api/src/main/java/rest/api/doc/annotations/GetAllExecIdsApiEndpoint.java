package rest.api.doc.annotations;

import static java.lang.annotation.ElementType.METHOD;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import rest.api.dto.ErrorResp;
import rest.api.dto.ScriptListResp;

@Operation(
      summary = "Get execution ids",
      tags = { "script list" }
)
@ApiResponses(value = {
      @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = {
                  @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ScriptListResp.class))
            }
      ),
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
public @interface GetAllExecIdsApiEndpoint {
}
