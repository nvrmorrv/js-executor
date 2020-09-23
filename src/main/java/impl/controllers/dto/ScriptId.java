package impl.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@AllArgsConstructor
@Getter
public class ScriptId extends RepresentationModel<ScriptId> {
  private final String id;
}
