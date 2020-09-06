package rest.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TimeoutErrorResp {
  private final String error;
  private final String output;
}
