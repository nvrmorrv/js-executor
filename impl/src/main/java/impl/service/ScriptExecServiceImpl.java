package impl.service;

import impl.repositories.ExecRepository;
import impl.repositories.entities.Execution;
import impl.service.dto.ExecInfo;
import impl.service.exceptions.DeletionException;
import impl.service.exceptions.ExecTimeOutException;
import impl.service.exceptions.UnknownIdException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ScriptExecServiceImpl implements ScriptExecService{
  private final ExecRepository repo;
  private final ScriptExecutor executor;

  public String executeScriptAsync(String script) {
    Execution exec = executor.executeAsync(script);
    return repo.addExecution(exec);
  }

  @SneakyThrows
  public ExecInfo executeScript(String script, long execTimeout, TimeUnit unit) {
    try {
      Execution exec = executor.execute(script, execTimeout, unit);
      return new ExecInfo(
            exec.getStatus().get().name(),
            getOutput(exec.getOutputStream()));
    } catch (TimeoutException ex) {
      throw new ExecTimeOutException(execTimeout, unit);
    }
  }

  public ExecInfo getExecutionStatus(String execId) {
    Execution exec = getExecOrThrow(execId);
    return new ExecInfo(
          exec.getStatus().get().name(),
          getOutput(exec.getOutputStream()));
  }

  @SneakyThrows
  public void cancelExecution(String execId) {
    Execution exec = getExecOrThrow(execId);
    executor.cancelExec(exec);
  }

  public void deleteExecution(String execId) {
    Execution exec = getExecOrThrow(execId);
    if(!isDoneStatus(exec.getStatus().get())) {
      throw new DeletionException(execId);
    }
    repo.removeExecution(execId);
  }

  public List<String> getFinishedExecutionIds() {
    return repo.getAllIds().stream()
          .filter(id -> isDoneStatus(
                getExecOrThrow(id).getStatus().get()))
          .collect(Collectors.toList());
  }

  public List<String> getAllExecutionIds() {
    return new ArrayList<>(repo.getAllIds());
  }

  private String getOutput(OutputStream outputStream) {
    return outputStream.toString();
  }

  private Execution getExecOrThrow(String execId) {
    return repo.getExecution(execId).orElseThrow(() -> new UnknownIdException(execId));
  }

  private boolean isDoneStatus(ExecStatus execStatus) {
    return ExecStatus.FINISHED.contains(execStatus);
  }

}
