package impl.repositories;

import impl.repositories.entities.Script;

import java.util.List;

public interface ScriptRepository {

  boolean addOrUpdateScript(String id, Script script);

  Script getScript(String scriptId);

  void removeScript(String scriptId);

  List<Script> getScripts();

  boolean contains(String scriptId);
}
