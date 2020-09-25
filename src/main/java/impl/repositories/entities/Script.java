package impl.repositories.entities;

import impl.service.dto.ScriptInfo;
import impl.shared.ExecStatus;
import org.springframework.data.annotation.Id;

import java.io.OutputStream;

public interface Script {

  @Id
  String getId();

  ExecStatus getStatus();

  byte[] getSource();

  byte[] getOutput();

  ScriptInfo getScriptInfo();

  void executeScript(OutputStream outputStream);

  void executeScript();

  void cancel();
}
