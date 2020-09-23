package impl.repositories.entities;

import impl.shared.ExecStatus;

import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.locks.Lock;

public interface Script {

  String getId();

  byte[] getSource();

  ExecStatus getStatus();

  String getScheduledTime();

  String getStartTime();

  String getFinishTime();

  byte[] getOutput();

  String getExMessage();

  List<String> getStackTrace();

  Lock getReadLock();

  void executeScript(OutputStream outputStream);

  void executeScript();

  void cancel();
}
