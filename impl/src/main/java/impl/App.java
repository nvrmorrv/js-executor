/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package impl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.SneakyThrows;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.mvc.WebContentInterceptor;
import rest.api.dto.StreamingExceptResp;

@SpringBootApplication
public class App {
  public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
   SpringApplication.run(App.class, args);
    //test6();
  }
  public static void test11() throws IOException {
    ByteArrayOutputStream arr = new ByteArrayOutputStream() {
      @SneakyThrows
      @Override
      public synchronized void write(int b) {
        super.write(b);
        System.out.println("int b");
      }

      @SneakyThrows
      @Override
      public synchronized void write(byte[] bytes) {
        super.write(bytes);
        System.out.println("write(byte[] bytes)");
      }

      @SneakyThrows
      @Override
      public synchronized void write(byte[] bytes, int off, int ln) {
        super.write(bytes, off, ln);
        System.out.println("write(byte[] bytes, int off, int ln)");
      }
    };
    try (Context ct = Context.newBuilder("js")
          .out(arr)
          .build()) {
      String prog = "var c = 0; while(c < 1000000) {console.log(c++);}";
      ct.eval(Source.newBuilder("js", prog, "blah").build());
    }
    System.out.println(arr.toString());
  }

  public static void test10() throws IOException {
    JsonGenerator generator = new JsonFactory().createGenerator(System.out);
    generator.writeStartObject();
    generator.writeStringField("output", "hello");
    generator.writeEndObject();
    generator.flush();
  }

  public static void test4() {
    ExecutorService pool = Executors.newFixedThreadPool(1);
    CompletableFuture<?> res = CompletableFuture.runAsync(() -> {
      System.out.println("start");
      long val = 0;
      while(val < 10000000) {
        val++;
      }
      System.out.println("done");
    }, pool);
    CompletableFuture<?> res2 = CompletableFuture.runAsync(() -> System.out.println("2"), pool);
    res2.cancel(true);
    pool.shutdown();
  }

  public static void test7() throws IOException {
    ByteArrayOutputStream arr = new ByteArrayOutputStream(); // synchronize on this
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    Context ct = Context.newBuilder("js")
          .out(arr)
          .err(err)
          .build();
    ct.close();
    String prog = "sdf.(234324)";
    ct.eval(Source.newBuilder("js", prog, "blah").build());
  }
  public static void test6() throws InterruptedException {// synchronize on this
    Context ct = Context.newBuilder("js")
          .build();
    Runnable runnable = () -> {
      try (ct) {
        String prog = "while(true){}";
        ct.eval(Source.newBuilder("js", prog, "blah").build());
      } catch (Exception e) {
        e.printStackTrace();
      }
    };
    Thread thread = new Thread(runnable);
    thread.start();
    Thread.sleep(3000);
    thread.interrupt();
  }


  public static void test5() throws ExecutionException, InterruptedException {
    ByteArrayOutputStream arr = new ByteArrayOutputStream(); // synchronize on this
    ByteArrayOutputStream err = new ByteArrayOutputStream(); // synchronize on this
    Context ct = Context.newBuilder("js")
          .out(arr)
          .err(err)
          .build();
    Runnable runnable = () -> {
      try (ct) {
        String prog = "console.log('yes'); throw 'exec'";
        ct.eval(Source.newBuilder("js", prog, "blah").build());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
    ExecutorService pool = Executors.newFixedThreadPool(1);
    CompletableFuture<Void> res = CompletableFuture.runAsync(runnable, pool);
    res.get();
    System.out.println("out: " + arr.toString());
    System.out.println("err: " + err.toString());
  }




  public static void test3() throws ExecutionException, InterruptedException {
    ExecutorService pool = Executors.newFixedThreadPool(1);
    CompletableFuture<?> future = CompletableFuture.runAsync(() -> {
      try{
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }, pool);
    CompletableFuture<?> future1 = CompletableFuture.runAsync(() -> {
      System.out.println("hello");
    }, pool);
    try {
      future1.cancel(true);
    } catch (Exception exception) {
      System.out.println(exception.getClass().getSimpleName());
      System.out.println(exception.getMessage());
    }
    future.get();
    future1.get();
    pool.shutdown();
  }

  public static void test() throws InterruptedException {

    ByteArrayOutputStream arr = new ByteArrayOutputStream(); // synchronize on this
    Context ct = Context.newBuilder("js")
         // .out(arr)
          .build();
    Runnable runnable = () -> {
      try (ct) {
        String prog = "while(true){console.log('yes')}";
        ct.eval(Source.newBuilder("js", prog, "blah").build());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
    ExecutorService pool = Executors.newFixedThreadPool(2);
    //Future<?> future =
    CompletableFuture<Void> res = CompletableFuture.runAsync(runnable, pool);
    Thread.sleep(3000);
    //future.cancel(true);
    res.cancel(true);
    Thread.sleep(2000);
    // System.out.println(res.get());
   // System.out.println(arr.toString());
  }

  public static void test1() throws InterruptedException, ExecutionException {
    ExecutorService fixed = Executors.newFixedThreadPool(1);

    Future<?> res = CompletableFuture.runAsync(() -> {
     // for (;;) {
        System.out.println("From first... ");
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      //}
    });

    Future<?> res2 = CompletableFuture.runAsync(() -> {
      System.out.println("yes");
    });

    System.out.println(res2.cancel(true));
    System.out.println(res.get());
    //Thread.sleep(5000);
  }
}


