package impl.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ExecInfo {
  private final String status;
  private final String output;
}
