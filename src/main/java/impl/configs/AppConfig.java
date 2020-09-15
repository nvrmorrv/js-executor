package impl.configs;

import java.util.concurrent.Executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import impl.controllers.interceptors.RequestCountInterceptor;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.WebContentInterceptor;
import org.zalando.problem.ProblemModule;
import org.zalando.problem.violations.ConstraintViolationProblemModule;

@Configuration
public class AppConfig {

  @Bean
  public WebMvcConfigurer webConfig(Executor applicationTaskExecutor,
                                    RequestCountInterceptor requestCountInterceptor) {
    return new WebMvcConfigurer() {
      @Override
      public void addInterceptors(InterceptorRegistry registry) {
        WebContentInterceptor interceptor = new WebContentInterceptor();
        interceptor.addCacheMapping(CacheControl.noStore().noTransform(), "/*");
        registry.addInterceptor(interceptor);
        registry.addInterceptor(requestCountInterceptor);
      }

      @Override
      public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(new ConcurrentTaskExecutor(applicationTaskExecutor));
      }
    };
  }

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper().registerModules(
     new ProblemModule(),
     new ConstraintViolationProblemModule());
  }

  @Bean
  public TimedAspect timedAspect(MeterRegistry registry) {
    return new TimedAspect(registry);
  }
}
