/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package impl;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
public class App {
  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
   // test1();
  }

  private static void test1() {
    ZonedDateTime time = ZonedDateTime.now(ZoneOffset.of("+14:00"));
    String str = time.format(DateTimeFormatter.ofPattern("uuuu-MM-dd;HH:mm:ss:SSS;O"));
    System.out.println(str);
  }

  private static void test2() {
    System.out.println(Charset.defaultCharset().displayName());
    String utf8 = "оірвмроро";
    System.out.println(utf8);
    String ascii = new String(utf8.getBytes(StandardCharsets.ISO_8859_1));
    System.out.println(ascii);
  }
}


