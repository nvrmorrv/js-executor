package impl.service.dto;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ExecInfo {
  private final String status;
  private final Optional<String> message;
}
