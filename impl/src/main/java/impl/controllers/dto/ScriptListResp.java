package impl.controllers.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ScriptListResp {
  private final List<ScriptId> scripts;
}
