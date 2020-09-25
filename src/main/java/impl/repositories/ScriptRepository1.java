package impl.repositories;

import impl.repositories.entities.Script;
import impl.repositories.exceptions.UnknownIdException;
import impl.shared.ExecStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ScriptRepository1 extends PagingAndSortingRepository<Script, String> {

  List<Script> findScriptsByStatus(ExecStatus status, Pageable pageable);

  List<Script> findScripts(Pageable pageable);

  default boolean addOrUpdateScript(String id, Script script) {
    boolean isNew = !existsById(id);
    save(script);
    return isNew;
  }

  default Script getScript(String scriptId) {
    return findById(scriptId).orElseThrow(() -> new UnknownIdException(scriptId));
  }

  default void removeScript(String scriptId) {
    findById(scriptId).orElseThrow(() -> new UnknownIdException(scriptId));
    deleteById(scriptId);
  }

  default boolean contains(String scriptId) {
    return existsById(scriptId);
  }
}
