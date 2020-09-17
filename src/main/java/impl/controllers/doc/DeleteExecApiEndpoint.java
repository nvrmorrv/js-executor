package impl.controllers.doc;

import static java.lang.annotation.ElementType.METHOD;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import impl.controllers.doc.resp.InternalSerErrResp;
import impl.controllers.doc.resp.NotFoundResp;
import impl.controllers.doc.resp.EmptyResp;
import org.springframework.http.MediaType;
import org.zalando.problem.Problem;

@Operation(
      summary = "Delete execution",
      tags = { "script" },
      parameters = {@Parameter(
            name = "id",
            in = ParameterIn.PATH,
            required = true
      )})
@ApiResponse(
        responseCode = "405",
        description = "Error: attempt to delete running execution",
        content = {
                @Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = Problem.class)) })
@EmptyResp(code = "204")
@NotFoundResp
@InternalSerErrResp
@Target({METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DeleteExecApiEndpoint {
}
