package impl.repositories.entities;

import impl.service.ExecStatus;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class Execution {
  private final AtomicReference<ExecStatus> status;
  private final OutputStream outputStream;
  private final CompletableFuture<Void> computation;
  private final CompletableFuture<Runnable> ctCreation;
}
