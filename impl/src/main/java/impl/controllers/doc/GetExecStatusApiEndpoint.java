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
import impl.controllers.dto.ExceptionResp;
import impl.controllers.dto.ExecStatusResp;
import org.springframework.http.MediaType;

@Operation(
      summary = "Get execution status",
      tags = { "script" },
      parameters = {@Parameter(
            name = "id",
            in = ParameterIn.PATH,
            required = true
      )})
@ApiResponse(
        responseCode = "200",
        description = "OK",
        content = {
                @Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(anyOf = {ExceptionResp.class, ExecStatusResp.class}))
        })
@NotFoundResp
@InternalSerErrResp
@Target({METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GetExecStatusApiEndpoint {
}
