package impl.service;

import impl.repositories.ScriptRepository;
import impl.repositories.entities.ExecStatus;
import impl.repositories.entities.Execution;
import impl.repositories.entities.Script;
import impl.service.dto.ExecInfo;
import impl.service.exceptions.DeletionException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ScriptExecServiceImpl {
  private final String lang;
  private final ScriptRepository repo;

  public ScriptExecServiceImpl(@Value("${executor.lang}")String lang, ScriptRepository repo) {
    this.lang = lang;
    this.repo = repo;
  }

  public void executeScriptAsync(String id, String scriptText) {
    Script script = new ScriptImpl(lang, id, scriptText);
    repo.addScript(id, script);
    script.executeAsync();
  }

  public void executeScript(String id, String scriptText, OutputStream respStream) {
    Script script = new ScriptImpl(lang, id, scriptText, respStream);
    repo.addScript(id, script);
    script.execute();
  }

  @SneakyThrows
  public ExecInfo getExecutionStatus(String execId) {
    Script script = repo.getScript(execId);
    Lock readLock = script.getReadLock();
    readLock.lock();
    ExecInfo info = getStatus(script);
    readLock.unlock();
    return info;
  }

  public String getExecutionScript(String id) {
    return repo.getScript(id).getScript();
  }

  public String getExecutionOutput(String id) {
    return getOutput(repo.getScript(id).getOutputStream());
  }

  @SneakyThrows
  public void cancelExecution(String execId) {
    Execution exec = repo.getScript(execId);
    executor.cancelExec(exec);
  }

  public void deleteExecution(String execId) {
    Execution exec = repo.getScript(execId);
    if(!exec.getComputation().isDone()) {
      throw new DeletionException(execId);
    }
    repo.removeScript(execId);
  }

  public List<String> getExecutionIds() {
    return new ArrayList<>(repo.getAllIds());
  }

  private String getOutput(OutputStream stream) {
    return stream.toString();
  }

  private ExecInfo getExecInfo(Script script) {
    if(script.getStatus() == ExecStatus.QUEUE) {
      return new ExecInfo(script.getId(), script.getStatus().name(), script.)
    }
  }
}
