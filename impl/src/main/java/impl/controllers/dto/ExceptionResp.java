package impl.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ExceptionResp implements ExecResp {
  private final String status;
  private final String message;
}
