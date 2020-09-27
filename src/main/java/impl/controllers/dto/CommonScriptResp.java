package impl.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

@AllArgsConstructor
@Getter
@Setter
@Relation(collectionRelation = "scripts")
public class CommonScriptResp extends ScriptResp {
  private final String id;
  private final String status;
  private final String createTime;
  private final String startTime;
  private final String finishTime;
}
