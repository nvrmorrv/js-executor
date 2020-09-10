package impl.service;

import impl.service.dto.ExecInfo;

import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface ScriptExecService {

  String executeScriptAsync(String script);

  void executeScript(String script, OutputStream stream);

  void checkScript(String script);

  ExecInfo getExecutionStatus(String execId);

  void cancelExecution(String execId);

  void deleteExecution(String execId);

  List<String> getExecutionIds();
}
