package rest.api.doc.annotations.resp;


import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import rest.api.dto.ErrorResp;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;

@ApiResponse(
        responseCode = "404",
        description = "Error: unknown id",
        content = {
                @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResp.class))
        })
@Target({ANNOTATION_TYPE, METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotFoundResp {
}
