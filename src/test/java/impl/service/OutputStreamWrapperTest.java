package impl.service;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OutputStreamWrapperTest {
  private ByteArrayOutputStream innerStream;
  private OutputStreamWrapper wrapperStream;

  @BeforeEach
  public void setStreams() {
    innerStream = new ByteArrayOutputStream();
    wrapperStream = new OutputStreamWrapper(innerStream);
  }

  @Test
  public void shouldWriteInt() {
    char c = 'b';
    wrapperStream.write(c);
    assertEquals(String.valueOf(c), innerStream.toString());
    assertEquals(String.valueOf(c), wrapperStream.toString());
  }

  @Test
  public void shouldWriteBytes() {
    String str = "hello";
    wrapperStream.write(str.getBytes());
    assertEquals(str, innerStream.toString());
    assertEquals(str, wrapperStream.toString());
  }

  @Test
  public void shouldWriteWithLength() {
    String str = "hello";
    wrapperStream.write(str.getBytes(), 0, str.length());
    assertEquals(str, innerStream.toString());
    assertEquals(str, wrapperStream.toString());
  }
}
