package impl.service;

import impl.repositories.ScriptRepository;
import impl.repositories.entities.Script;
import impl.shared.ExecStatus;
import impl.service.dto.*;
import impl.service.exceptions.DeletionException;

import java.io.OutputStream;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    Script script = repo.getScript(id);
    return getScriptInfo(script);
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
    repo.getScript(id).cancel();
  }

  @Override
  public synchronized void deleteScript(String execId) {
    if(isNotFinished(repo.getScript(execId).getStatus())) {
      throw new DeletionException(execId);
    }
    repo.removeScript(execId);
  }


  @Override
  public List<ScriptInfo> getScripts() {
    return repo.getScripts().stream()
          .map(this::getScriptInfo)
          .collect(Collectors.toList());
  }

  private ScriptInfo getScriptInfo(Script script) {
    script.getReadLock().lock();
    ScriptInfo info = new ScriptInfo(
          script.getId(),
          script.getStatus(),
          script.getScheduledTime(),
          script.getStartTime(),
          script.getFinishTime(),
          script.getExMessage(),
          script.getStackTrace());
    script.getReadLock().unlock();
    return info;
  }

  private void checkScriptForCompleteness(String id) {
    if(repo.contains(id) && isNotFinished(repo.getScript(id).getStatus())) {
      throw new DeletionException(id);
    }
  }

  private boolean isNotFinished(ExecStatus status) {
    return !ExecStatus.FINISHED.contains(status);
  }
}
