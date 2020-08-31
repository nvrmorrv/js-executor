package impl.service.exceptions;

public class DeletionException extends RuntimeException {

  public DeletionException(String id) {
    super(generateMessage(id));
  }

  public static String generateMessage(String id) {
    return "To delete the script it should be done or canceled, id: " + id;
  }
}
