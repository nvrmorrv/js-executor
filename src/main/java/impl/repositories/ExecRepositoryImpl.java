package impl.repositories;

import impl.repositories.entities.Execution;
import impl.service.exceptions.UnknownIdException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

@Component
public class ExecRepositoryImpl implements ExecRepository {
  private final Map<String, Execution> map;

  public ExecRepositoryImpl(MeterRegistry registry) {
    map = registry.gaugeMapSize("map_size", Tags.empty(), new ConcurrentHashMap<>());
  }

  @Override
  public String addExecution(Execution execution) {
    String id = UUID.randomUUID().toString();
    map.put(id, execution);
    return id;
  }

  @Override
  public Optional<Execution> getExecution(String execId) {
    return Optional.ofNullable(map.get(execId));
  }

  @Override
  public void removeExecution(String execId) {
    Optional.ofNullable(map.remove(execId)).orElseThrow(() -> new UnknownIdException(execId));
  }

  @Override
  public Set<String> getAllIds() {
    return map.keySet();
  }
}
