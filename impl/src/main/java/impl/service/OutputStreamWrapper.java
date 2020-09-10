package impl.service;

import lombok.AllArgsConstructor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

@AllArgsConstructor
public class OutputStreamWrapper extends OutputStream {
  private final OutputStream stream;
  private final AtomicReference<String> output = new AtomicReference<>("");

  @Override
  public synchronized void write(int b) throws IOException {
    output.updateAndGet(s -> s + b);
    stream.write(b);
  }

  @Override
  public synchronized void write(byte[] bytes) throws IOException {
    output.updateAndGet(s -> s + bytes);
    stream.write(bytes);
  }

  @Override
  public synchronized void write(byte[] bytes, int off, int ln) throws IOException {
    write(bytes);
  }

  @Override
  public synchronized String toString() {
    return output.toString();
  }

}
