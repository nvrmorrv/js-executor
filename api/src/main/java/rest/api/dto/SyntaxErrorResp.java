package rest.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class SyntaxErrorResp {
  private final String error;
  private final String desc;
  private final String section;
}
