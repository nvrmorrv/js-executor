package impl.controllers.doc.resp;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.MediaType;
import org.zalando.problem.Problem;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;

@ApiResponse(
        responseCode = "500",
        description = "Error: server error",
        content = {
                @Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = Problem.class))
        })
@Target({ANNOTATION_TYPE, METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface InternalSerErrResp {
}
