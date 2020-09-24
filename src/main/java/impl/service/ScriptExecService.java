package impl.service;

import impl.service.dto.ScriptInfo;

import impl.service.dto.SortParams;
import java.io.OutputStream;
import java.util.List;
import java.util.TimeZone;

public interface ScriptExecService {

  boolean createScript(String id, byte[] scriptText, TimeZone timeZone);

  void executeScriptAsync(String id);

  void executeScript(String id, OutputStream respStream);

  ScriptInfo getScriptInfo(String id);

  byte[] getScriptSource(String id);

  byte[] getScriptOutput(String id);

  void cancelScriptExecution(String execId);

  void deleteScript(String execId);

  List<ScriptInfo> getScripts(SortParams sortParams);
}
