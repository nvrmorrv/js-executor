package impl.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class ExceptionStatusResp extends StatusResp {
  private final String id;
  private final String status;
  private final String createTime;
  private final String startTime;
  private final String finishTime;
  private final String message;
  private final List<String> stackTrace;
}
