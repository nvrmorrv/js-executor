package impl.service;

import impl.shared.ScriptInfo;

import java.io.OutputStream;
import java.util.TimeZone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ScriptExecService {

  boolean createScript(String id, String owner, byte[] source, TimeZone timeZone);

  void executeScriptAsync(String id);

  void executeScript(String id, OutputStream respStream);

  ScriptInfo getScriptInfo(String id);

  byte[] getScriptSource(String id);

  byte[] getScriptOutput(String id);

  void cancelScriptExecution(String id);

  void deleteScript(String id);

  Page<ScriptInfo> getScriptInfoPage(Pageable pageable, String filterStatus);
}
