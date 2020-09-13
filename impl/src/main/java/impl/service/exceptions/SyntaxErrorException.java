package impl.service.exceptions;

import lombok.Getter;

@Getter
public class SyntaxErrorException extends RuntimeException {
  private final String section;

  public SyntaxErrorException(String message, String section) {
    super(message);
    this.section = section;
  }
}
