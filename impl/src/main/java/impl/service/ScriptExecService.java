package impl.service;

import impl.service.dto.ExecInfo;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface ScriptExecService {

  String executeScriptAsync(String script);

  ExecInfo executeScript(String script, long timeout, TimeUnit timeUnit);

  ExecInfo getExecutionStatus(String execId);

  void cancelExecution(String execId);

  void deleteExecution(String execId);

  List<String> getExecutionIds();
}
