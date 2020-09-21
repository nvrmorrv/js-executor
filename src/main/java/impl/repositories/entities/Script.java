package impl.repositories.entities;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

public interface Script {
  String getId();

  String getScript();

  ExecStatus getStatus();

  byte[] getOutput();

  byte[] getErrOutput();

  Optional<String> getExMessage();

  Optional<List<String>> getStackTrace();

  Lock getReadLock();

  void execute();

  void executeAsync();

  void cancel();
}
