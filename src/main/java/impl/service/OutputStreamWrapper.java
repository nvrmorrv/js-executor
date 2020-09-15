package impl.service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OutputStreamWrapper extends ByteArrayOutputStream {
  private final OutputStream respOutStream;

  @Override
  public void write(int b) {
    super.write(b);
    writeToRespStream(new byte[]{(byte)b}, 0, 1);
  }

  @SneakyThrows
  @Override
  public void write(byte[] bytes) {
    super.write(bytes);
  }

  @Override
  public void write(byte[] bytes, int off, int ln) {
    super.write(bytes, off, ln);
    writeToRespStream(bytes, off, ln);
  }

  @SneakyThrows
  private void writeToRespStream(byte[] bytes, int off, int ln) {
    respOutStream.write(bytes, off, ln);
    respOutStream.flush();
  }
}