package impl.repositories;

import impl.repositories.entities.Script;
import impl.repositories.exceptions.UnknownIdException;
import impl.shared.ExecStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScriptRepository1 extends PagingAndSortingRepository<Script, String> {

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

  default List<Script> getScripts(Pageable pageable, Optional<ExecStatus> status) {
    return findAll(pageable)
          .getContent()
          .stream()
          .filter();
  }

  default boolean contains(String scriptId) {
    return existsById(scriptId);
  }

}
