package impl.controllers.utils;

import impl.controllers.dto.CommonScriptResp;
import impl.controllers.dto.ExceptionScriptResp;
import impl.shared.ScriptInfo;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Optional;

public class ResponseMapper {
  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS;dd-MM-uuuu;O");

  public static CommonScriptResp getCommonStatusResp(ScriptInfo info) {
    return new CommonScriptResp(
          info.getId(),
          info.getOwner(),
          info.getStatus().name(),
          info.getCreateTime().format(formatter),
          getStringFromDate(info.getStartTime()),
          getStringFromDate(info.getFinishTime()));
  }

  public static ExceptionScriptResp getExceptionStatusResp(ScriptInfo info) {
    return new ExceptionScriptResp(
          info.getId(),
          info.getOwner(),
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
