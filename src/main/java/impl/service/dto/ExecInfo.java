package impl.service.dto;

import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ExecInfo {
  private final String id;
  private final String status;
  private final String scheduledTime;
  private final String startTime;
  private final String endTime;
  private final Optional<String> message;
  private final Optional<List<String>> stackTrace;
}
