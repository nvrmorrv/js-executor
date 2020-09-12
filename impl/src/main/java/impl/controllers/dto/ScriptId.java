package impl.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.hateoas.RepresentationModel;

@AllArgsConstructor
@Getter
public class ScriptId extends RepresentationModel<ExceptionStatusResp> {
  private final String id;
}
