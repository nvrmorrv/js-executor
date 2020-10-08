package impl.repositories.entities;

import impl.shared.ScriptInfo;
import impl.shared.ScriptStatus;
import java.io.OutputStream;

public interface Script {

  String getId();

  String getOwner();

  ScriptStatus getStatus();

  byte[] getSource();

  byte[] getOutput();

  ScriptInfo getScriptInfo();

  void executeScript(OutputStream outputStream);

  void executeScript();

  void cancelExecution();
}
