package impl.repositories;

import impl.repositories.entities.Script;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import impl.repositories.exceptions.UnknownIdException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

@Component
public class ScriptRepositoryImpl implements ScriptRepository {
  private final Map<String, Script> map;

  public ScriptRepositoryImpl(MeterRegistry registry) {
    map = registry.gaugeMapSize("map_size", Tags.empty(), new ConcurrentHashMap<>());
  }

  @Override
  public String addScript(String id, Script script) {
    map.put(id, script);
    return id;
  }

  @Override
  public Script getScript(String scriptId) {
    return Optional.ofNullable(map.get(scriptId)).orElseThrow(() -> new UnknownIdException(scriptId));
  }

  @Override
  public void removeScript(String scriptId) {
    Optional.ofNullable(map.remove(scriptId)).orElseThrow(() -> new UnknownIdException(scriptId));
  }

  @Override
  public List<Script> getScripts() {
    return new ArrayList<>(map.values());
  }
}
