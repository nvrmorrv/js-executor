package impl.repositories.entities;

import impl.service.dto.ScriptInfo;
import impl.shared.ExecStatus;
import java.io.OutputStream;

public interface Script {

  String getId();

  ExecStatus getStatus();

  byte[] getSource();

  byte[] getOutput();

  ScriptInfo getScriptInfo();

  void executeScript(OutputStream outputStream);

  void executeScript();

  void cancel();
}
