package impl.controllers.doc;

import static java.lang.annotation.ElementType.METHOD;

import impl.controllers.doc.resp.InternalSerErrResp;
import impl.controllers.doc.resp.NotFoundResp;
import impl.controllers.doc.resp.PlainTextResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Operation(
      summary = "Get execution script",
      tags = { "script" },
      parameters = {@Parameter(
            name = "id",
            in = ParameterIn.PATH,
            required = true
      )})
@PlainTextResp
@NotFoundResp
@InternalSerErrResp
@Target({METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GetExecScriptApiEndpoint {
}
