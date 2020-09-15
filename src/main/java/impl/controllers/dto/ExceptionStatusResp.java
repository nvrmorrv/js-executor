package impl.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ExceptionStatusResp extends StatusResp {
  private final String status;
  private final String message;
}