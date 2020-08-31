package impl.service.exceptions;

import java.util.concurrent.TimeUnit;

public class ExecTimeOutException extends RuntimeException {
  public ExecTimeOutException(long timeout, TimeUnit unit) {
    super(generateMessage(timeout, unit));
  }

  public static String generateMessage(long timeout, TimeUnit unit) {
    return String.format("Time is over. Timeout: %1d %2s", timeout, unit.toString());
  }
}
