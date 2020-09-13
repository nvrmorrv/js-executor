package impl.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CommonStatusResp extends StatusResp {
  private final String status;
}
