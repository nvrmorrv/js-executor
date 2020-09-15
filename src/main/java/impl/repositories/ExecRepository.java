package impl.repositories;

import impl.repositories.entities.Execution;
import io.micrometer.core.annotation.Timed;

import java.util.Optional;
import java.util.Set;

public interface ExecRepository {

  String addExecution(Execution execution);

  Optional<Execution> getExecution(String execId);

  void removeExecution(String execId);

  Set<String> getAllIds();
}
