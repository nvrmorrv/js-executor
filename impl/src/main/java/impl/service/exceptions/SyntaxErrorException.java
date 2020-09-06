package impl.service.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class SyntaxErrorException extends RuntimeException {
  private final String message = "Syntax error";
  private final String desc;
  private final String section;
}
