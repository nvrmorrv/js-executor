package impl.controllers.exceptions;

public class CancellationException extends RuntimeException {
  public CancellationException() {
    super("Passed status should be CANCELLED");
  }
}
