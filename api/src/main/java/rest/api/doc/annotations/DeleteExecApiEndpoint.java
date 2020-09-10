package rest.api.doc.annotations;

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

import rest.api.doc.annotations.resp.InternalSerErrResp;
import rest.api.doc.annotations.resp.NotFoundResp;
import rest.api.doc.annotations.resp.OkEmptyResp;
import rest.api.dto.ErrorResp;

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
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResp.class)) })
@OkEmptyResp
@NotFoundResp
@InternalSerErrResp
@Target({METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DeleteExecApiEndpoint {
}
