package impl.shared;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ScriptInfo implements Serializable {
  private final String id;
  private final ScriptStatus status;
  private final ZonedDateTime createTime;
  private final Optional<ZonedDateTime> startTime;
  private final Optional<ZonedDateTime> finishTime;
  private final Optional<String> message;
  private final Optional<List<String>> stackTrace;
}
