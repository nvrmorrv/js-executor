package impl.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import impl.repositories.ExecRepository;
import impl.repositories.entities.Execution;
import impl.service.dto.ExecInfo;
import impl.service.exceptions.DeletionException;
import impl.service.exceptions.ExceptResException;
import impl.service.exceptions.UnknownIdException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

  @SneakyThrows
  public void executeScript(String script, OutputStream stream) {
    Execution exec = getExec(script, stream);
    String id = repo.addExecution(exec);
    writeId(id, stream);
    executor.execute(
          exec.getScript(),
          exec.getStatus(),
          exec.getCtCreation(),
          exec.getComputation(),
          exec.getOutputStream());
  }

  @Override
  public void checkScript(String script) {
    executor.checkScript(script);
  }

  @SneakyThrows
  public ExecInfo getExecutionStatus(String execId) {
    Execution exec = getExecOrThrow(execId);
    if(exec.getComputation().isDone()) {
      try {
        exec.getComputation().get();
      } catch (ExecutionException ex) {
        ExceptResException e = (ExceptResException) ex.getCause();
        return new ExecInfo(ExecStatus.DONE_WITH_EXCEPTION.name(), e.getOutput(),
              Optional.of(e.getExceptionMessage()));
      }
    }
    return new ExecInfo(
          exec.getStatus().get().name(),
          ScriptExecutor.getOutput(exec.getOutputStream()),
          Optional.empty());
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
    repo.removeExecution(execId);
  }

  public List<String> getExecutionIds() {
    return new ArrayList<>(repo.getAllIds());
  }

  private Execution getExec(String script) {
    executor.checkScript(script);
    AtomicReference<impl.service.ExecStatus> status = new AtomicReference<>(impl.service.ExecStatus.QUEUE);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    CompletableFuture<Runnable> ctCreation = new CompletableFuture<>();
    CompletableFuture<Void> comp = executor.executeAsync(script, status, ctCreation, outputStream);
    return new Execution(script, status, outputStream, comp, ctCreation);
  }

  private Execution getExec(String script, OutputStream outputStream) {
    executor.checkScript(script);
    AtomicReference<impl.service.ExecStatus> status = new AtomicReference<>(impl.service.ExecStatus.QUEUE);
    OutputStreamWrapper streamWrapper = new OutputStreamWrapper(outputStream);
    CompletableFuture<Runnable> ctCreation = new CompletableFuture<>();
    CompletableFuture<Void> comp = new CompletableFuture<>();
    return new Execution(script, status, streamWrapper, comp, ctCreation);
  }

  private Execution getExecOrThrow(String execId) {
    return repo.getExecution(execId).orElseThrow(() -> new UnknownIdException(execId));
  }

  private void writeId(String id, OutputStream outputStream) throws IOException {
    JsonGenerator generator = new JsonFactory().createGenerator(outputStream);
    generator.writeStartObject();
    generator.writeStringField("id", id);
    generator.writeEndObject();
    generator.flush();
    outputStream.write('\n');
    outputStream.flush();
  }

}
