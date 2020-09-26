package impl.service.exceptions;

public class PaginationException extends RuntimeException {
  public PaginationException(String message) {
    super(message);
  }
}
