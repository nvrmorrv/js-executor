package impl.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class ScriptInfo {
  private final String id;
  private final String status;
  private final String scheduledTime;
  private final String startTime;
  private final String finishTime;
  private final String message;
  private final List<String> stackTrace;
}
