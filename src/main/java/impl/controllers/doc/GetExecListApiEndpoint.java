package impl.controllers.doc;

import static java.lang.annotation.ElementType.METHOD;

import impl.controllers.dto.ScriptId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.links.Link;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import impl.controllers.doc.resp.InternalSerErrResp;
import org.springframework.http.MediaType;

@Operation(
      summary = "Get execution list",
      tags = { "script list" }
)
@ApiResponse(
      responseCode = "200",
      description = "OK",
      content = {
            @Content(
                  mediaType = MediaType.APPLICATION_JSON_VALUE,
                  array = @ArraySchema(
                          schema = @Schema(implementation = ScriptId.class)
                  ))})
@InternalSerErrResp
@Target({METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GetExecListApiEndpoint {
}
