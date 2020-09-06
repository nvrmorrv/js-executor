package impl.service.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ExceptionResult implements ExecInfo{
  private final String status;
  private final String message;
  private final String output;
}
