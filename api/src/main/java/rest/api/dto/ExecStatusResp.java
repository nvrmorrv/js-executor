package rest.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ExecStatusResp implements ExecResp {
  private final String status;
  private final String output;
}
