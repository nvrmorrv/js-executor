package impl.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ExecStatusResp implements ExecResp {
  private final String status;
}
