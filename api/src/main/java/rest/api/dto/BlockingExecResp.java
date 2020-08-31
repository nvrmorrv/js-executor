package rest.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class BlockingExecResp {
  private final String resultStatus;
  private final String output;
}
