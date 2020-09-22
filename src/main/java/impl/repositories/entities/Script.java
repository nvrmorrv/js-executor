package impl.repositories.entities;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

public interface Script {

  String getId();

  byte[] getScript();

  ExecStatus getStatus();

  String getScheduledTime();

  String getStartTime();

  String getFinishTime();

  byte[] getOutput();

  byte[] getErrOutput();

  String getExMessage();

  List<String> getStackTrace();

  Lock getReadLock();

  void execute(OutputStream outputStream);

  void executeAsync();

  void cancel();
}
