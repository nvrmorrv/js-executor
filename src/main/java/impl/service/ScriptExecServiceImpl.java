package impl.service;

import static impl.service.PagingAndSortingService.getSortedPage;

import impl.repositories.ScriptRepository;
import impl.repositories.entities.Script;
import impl.shared.ScriptStatus;
import impl.service.exceptions.DeletionException;

import impl.shared.ScriptInfo;
import java.io.OutputStream;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ScriptExecServiceImpl implements ScriptExecService{
  private final String lang;
  private final ScriptRepository repo;

  public ScriptExecServiceImpl(@Value("${executor.lang}")String lang, ScriptRepository repo) {
    this.lang = lang;
    this.repo = repo;
  }

  @Override
  public synchronized boolean createScript(String id, byte[] scriptText, TimeZone timeZone) {
    checkScriptForCompleteness(id);
    Script script = new ScriptImpl(lang, id, scriptText, timeZone);
    return repo.addOrUpdateScript(id, script);
  }

  @Override
  public void executeScript(String id, OutputStream outputStream) {
    repo.getScript(id).executeScript(outputStream);
  }

  @Async
  @Override
  public void executeScriptAsync(String id) {
    repo.getScript(id).executeScript();
  }

  @Override
  public ScriptInfo getScriptInfo(String id) {
    return repo.getScript(id).getScriptInfo();
  }

  @Override
  public byte[] getScriptSource(String id) {
    return repo.getScript(id).getSource();
  }

  @Override
  public byte[] getScriptOutput(String id) {
    return repo.getScript(id).getOutput();
  }

  @Override
  public void cancelScriptExecution(String id) {
    repo.getScript(id).cancelExecution();
  }

  @Override
  public synchronized void deleteScript(String id) {
    if(isNotFinished(repo.getScript(id).getStatus())) {
      throw new DeletionException(id);
    }
    repo.removeScript(id);
  }

  @Override
  public Page<ScriptInfo> getScriptInfoPage(Pageable pageable, String filterStatus) {
    List<ScriptInfo> list = repo.getScripts().stream()
          .map(Script::getScriptInfo)
          .collect(Collectors.toList());
    return getSortedPage(list, pageable, filterStatus);
  }

  private void checkScriptForCompleteness(String id) {
    if(repo.contains(id) && isNotFinished(repo.getScript(id).getStatus())) {
      throw new DeletionException(id);
    }
  }

  private boolean isNotFinished(ScriptStatus status) {
    return !ScriptStatus.FINISHED.contains(status);
  }
}
