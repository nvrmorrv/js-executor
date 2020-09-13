package impl.service.exceptions;

public class UnknownIdException extends RuntimeException {

  public UnknownIdException(String id) {
    super(generateMessage(id));
  }

  public static String generateMessage(String id) {
    return "There is no such id, id: " + id;
  }
}
