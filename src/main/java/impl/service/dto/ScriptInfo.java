package impl.service.dto;

import impl.shared.ExecStatus;
import java.time.ZonedDateTime;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class ScriptInfo {
  private final String id;
  private final ExecStatus status;
  private final ZonedDateTime createTime;
  private final Optional<ZonedDateTime> startTime;
  private final Optional<ZonedDateTime> finishTime;
  private final Optional<String> message;
  private final Optional<List<String>> stackTrace;
}
