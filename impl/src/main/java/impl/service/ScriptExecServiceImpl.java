package impl.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import impl.repositories.ExecRepository;
import impl.repositories.entities.Execution;
import impl.service.dto.ExecInfo;
import impl.service.exceptions.DeletionException;
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
import java.util.function.Function;
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
  private final ObjectMapper jsonMapper;

  public String executeScriptAsync(String script) {
    Execution exec = getExec(script);
    return repo.addExecution(exec);
  }

  @SneakyThrows
  public void executeScript(String script,
                            OutputStream stream,
                            Function<String, Object> idDtoProvider) {
    Execution exec = getExec(script, stream);
    String id = repo.addExecution(exec);
    Object idDto = idDtoProvider.apply(id);
    writeId(idDto, stream);
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
    return ScriptExecutor.getOutput(getExecOrThrow(id).getOutputStream());
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

  private void writeId(Object idDto, OutputStream outputStream) throws IOException {
    JsonGenerator generator = new JsonFactory().createGenerator(outputStream);
    jsonMapper.writeValue(generator, idDto);
    generator.flush();
    outputStream.write('\n');
    outputStream.flush();
  }

}
