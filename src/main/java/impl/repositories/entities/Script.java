package impl.repositories.entities;

import impl.shared.ScriptInfo;
import impl.shared.ExecStatus;
import java.io.OutputStream;
import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.annotation.KeySpace;

@KeySpace
public interface Script {
  @Id
  String getId();

  ExecStatus getStatus();

  byte[] getSource();

  byte[] getOutput();

  @KeySpace
  ScriptInfo getScriptInfo();

  void executeScript(OutputStream outputStream);

  void executeScript();

  void cancel();
}
