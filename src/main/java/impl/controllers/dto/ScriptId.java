package impl.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.hateoas.RepresentationModel;

@AllArgsConstructor
@Getter
public class ScriptId extends RepresentationModel<ScriptId> {
  private final String id;
}
