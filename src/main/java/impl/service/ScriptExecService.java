package impl.service;

import impl.service.dto.ScriptInfo;

import java.io.OutputStream;
import java.util.List;
import java.util.TimeZone;

public interface ScriptExecService {

  boolean createScript(String id, byte[] scriptText, TimeZone timeZone);

  void executeScriptAsync(String id);

  void executeScript(String id, OutputStream respStream);

  ScriptInfo getScriptInfo(String id);

  byte[] getScriptText(String id);

  byte[] getScriptOutput(String id);

  byte[] getScriptErrOutput(String id);

  void cancelScriptExecution(String execId);

  void deleteScript(String execId);

  List<ScriptInfo> getScripts();

  boolean isExist(String id);
}
