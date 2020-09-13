package impl.configs;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.WebContentInterceptor;
import org.zalando.problem.ProblemModule;
import org.zalando.problem.violations.ConstraintViolationProblemModule;

@Configuration
@EnableAsync
public class AppConfig implements AsyncConfigurer {

  @Bean
  public WebMvcConfigurer webConfig(ConcurrentTaskExecutor concurrentTaskExecutor) {
    return new WebMvcConfigurer() {
      @Override
      public void addInterceptors(InterceptorRegistry registry) {
        WebContentInterceptor interceptor = new WebContentInterceptor();
        interceptor.addCacheMapping(CacheControl.noStore().noTransform(), "/*");
        registry.addInterceptor(interceptor);
      }

      @Override
      public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(concurrentTaskExecutor);
      }
    };
  }

  @Bean
  protected ConcurrentTaskExecutor getTaskExecutor(@Qualifier("taskExecutor") Executor taskExecutor) {
    return new ConcurrentTaskExecutor(taskExecutor);
  }

  @Bean(name = "taskExecutor")
  @Override
  public Executor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(500);
    executor.initialize();
    return executor;
  }


  @Bean(name = "AsyncExecutor")
  public ExecutorService service(@Value("${executor.thread-count}") int threadCount) {
    return Executors.newFixedThreadPool(threadCount);
  }

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper().registerModules(
     new ProblemModule(),
     new ConstraintViolationProblemModule());
  }
}
