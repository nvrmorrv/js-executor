package impl.service;

import impl.service.dto.ExecInfo;

import java.io.OutputStream;
import java.util.List;

public interface ScriptExecService {

  String executeScriptAsync(String script);

  String createExec(String script, OutputStream stream);

  void executeScript(String id);

  ExecInfo getExecutionStatus(String execId);

  String getExecutionScript(String execId);

  String getExecutionOutput(String execId);

  void cancelExecution(String execId);

  void deleteExecution(String execId);

  List<String> getExecutionIds();
}
