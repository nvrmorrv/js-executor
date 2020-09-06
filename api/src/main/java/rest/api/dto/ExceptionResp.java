package rest.api.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ExceptionResp implements ExecResp {
  private final String status;
  private final String message;
  private final String output;
}
