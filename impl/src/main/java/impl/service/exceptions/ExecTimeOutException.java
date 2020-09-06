package impl.service.exceptions;

import java.util.concurrent.TimeUnit;
import lombok.Getter;

@Getter
public class ExecTimeOutException extends RuntimeException {
  private final String output;

  public ExecTimeOutException(long timeout, TimeUnit unit, String output) {
    super(generateMessage(timeout, unit));
    this.output = output;
  }

  public static String generateMessage(long timeout, TimeUnit unit) {
    return String.format("Time is over. Timeout: %1d %2s", timeout, unit.toString());
  }
}
