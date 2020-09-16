package impl.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@AllArgsConstructor
@Getter
@Relation(collectionRelation = "scripts")
public class ScriptId extends RepresentationModel<ExceptionStatusResp> {
  private final String id;
}
