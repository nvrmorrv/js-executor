package impl.controllers.doc.resp;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.MediaType;

@ApiResponse(
      responseCode = "200",
      description = "OK",
      content = {
            @Content(
                  mediaType = MediaType.TEXT_PLAIN_VALUE,
                  schema = @Schema(implementation = String.class))
      }
)
@Target({ANNOTATION_TYPE, METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PlainTextResp {
}
