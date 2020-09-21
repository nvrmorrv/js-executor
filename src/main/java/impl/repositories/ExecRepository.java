package impl.repositories;

import impl.repositories.entities.Execution;

import java.util.Optional;
import java.util.Set;

public interface ExecRepository {

  String addExecution(Execution execution);

  Optional<Execution> getExecution(String execId);

  Optional<Execution> removeExecution(String execId);

  Set<String> getAllIds();
}
