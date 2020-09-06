package impl.service.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ExceptResException extends RuntimeException {
  private final String exceptionMessage;
  private final String output;
}
