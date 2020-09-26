package impl.controllers.utils;

import impl.controllers.dto.CommonStatusResp;
import impl.controllers.dto.ExceptionStatusResp;
import impl.shared.ScriptInfo;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Optional;

public class ResponseMapper {
  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS;dd-MM-uuuu;O");

  public static CommonStatusResp getCommonStatusResp(ScriptInfo info) {
    return new CommonStatusResp(
          info.getId(),
          info.getStatus().name(),
          info.getCreateTime().format(formatter),
          getStringFromDate(info.getStartTime()),
          getStringFromDate(info.getFinishTime()));
  }

  public static ExceptionStatusResp getExceptionStatusResp(ScriptInfo info) {
    return new ExceptionStatusResp(
          info.getId(),
          info.getStatus().name(),
          info.getCreateTime().format(formatter),
          getStringFromDate(info.getStartTime()),
          getStringFromDate(info.getFinishTime()),
          info.getMessage().orElse(""),
          info.getStackTrace().orElse(Collections.emptyList()));
  }

  private static String getStringFromDate(Optional<ZonedDateTime> dateTime) {
    return dateTime.map(zonedDateTime -> zonedDateTime.format(formatter)).orElse("");
  }
}
