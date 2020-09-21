package impl.service;

import impl.repositories.ExecRepository;
import impl.repositories.entities.ExecStatus;
import impl.repositories.entities.Execution;
import impl.service.dto.ExecInfo;
import impl.service.exceptions.DeletionException;
import impl.repositories.exceptions.UnknownIdException;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ScriptExecServiceImpl implements ScriptExecService {
  private final ExecRepository repo;
  private final ScriptExecutor executor;

  public String executeScriptAsync(String script) {
    Execution exec = getExec(script);
    return repo.addExecution(exec);
  }

  public String createExec(String script, OutputStream stream) {
    Execution exec = getExec(script, stream);
    return repo.addExecution(exec);
  }

  @SneakyThrows
  public void executeScript(String id) {
    Execution exec = getExecOrThrow(id);
    if(exec.getStatus().get() != ExecStatus.CREATED) {
      throw new IllegalArgumentException("This method for scripts with CREATED status only");
    }
    exec.getStatus().set(ExecStatus.QUEUE);
    executor.execute(
          exec.getScript(),
          exec.getStatus(),
          exec.getCtCreation(),
          exec.getComputation(),
          exec.getOutputStream());
  }

  @SneakyThrows
  public ExecInfo getExecutionStatus(String execId) {
    Execution exec = getExecOrThrow(execId);
    if(exec.getComputation().isDone()) {
      try {
        exec.getComputation().get();
      } catch (ExecutionException ex) {
        return new ExecInfo(ExecStatus.DONE_WITH_EXCEPTION.name(),
              Optional.of(ex.getCause().getMessage()));
      }
    }
    return new ExecInfo(exec.getStatus().get().name(), Optional.empty());
  }

  public String getExecutionScript(String id) {
    return getExecOrThrow(id).getScript();
  }

  public String getExecutionOutput(String id) {
    return getOutput(getExecOrThrow(id).getOutputStream());
  }

  @SneakyThrows
  public void cancelExecution(String execId) {
    Execution exec = getExecOrThrow(execId);
    executor.cancelExec(exec);
  }

  public void deleteExecution(String execId) {
    Execution exec = getExecOrThrow(execId);
    if(!exec.getComputation().isDone()) {
      throw new DeletionException(execId);
    }
    repo.removeExecution(execId).orElseThrow(() -> new UnknownIdException(execId));
  }

  public List<String> getExecutionIds() {
    return new ArrayList<>(repo.getAllIds());
  }

  private Execution getExec(String script) {
    executor.checkScript(script);
    AtomicReference<ExecStatus> status = new AtomicReference<>(ExecStatus.QUEUE);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    CompletableFuture<Runnable> ctCreation = new CompletableFuture<>();
    CompletableFuture<Void> comp = executor.executeAsync(script, status, ctCreation, outputStream);
    return new Execution(script, status, outputStream, comp, ctCreation);
  }

  private Execution getExec(String script, OutputStream outputStream) {
    executor.checkScript(script);
    AtomicReference<ExecStatus> status = new AtomicReference<>(ExecStatus.CREATED);
    OutputStreamWrapper streamWrapper = new OutputStreamWrapper(outputStream);
    CompletableFuture<Runnable> ctCreation = new CompletableFuture<>();
    CompletableFuture<Void> comp = new CompletableFuture<>();
    return new Execution(script, status, streamWrapper, comp, ctCreation);
  }

  private Execution getExecOrThrow(String execId) {
    return repo.getExecution(execId).orElseThrow(() -> new UnknownIdException(execId));
  }

  private String getOutput(OutputStream stream) {
    return stream.toString();
  }
}
