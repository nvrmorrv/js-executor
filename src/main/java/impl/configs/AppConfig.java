package impl.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.prometheus.PrometheusTimer;
import java.util.Arrays;
import java.util.stream.StreamSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.WebContentInterceptor;
import org.zalando.problem.ProblemModule;
import org.zalando.problem.violations.ConstraintViolationProblemModule;

@Configuration
public class AppConfig {

  @Bean
  public WebMvcConfigurer webConfig() {
    return new WebMvcConfigurer() {
      @Override
      public void addInterceptors(InterceptorRegistry registry) {
        WebContentInterceptor interceptor = new WebContentInterceptor();
        interceptor.addCacheMapping(CacheControl.noStore().noTransform(), "/*");
        registry.addInterceptor(interceptor);
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
  public Timer timer(MeterRegistry meterRegistry) {
    Timer timer = Timer.builder("running_time").publishPercentileHistogram().register(meterRegistry);
    meterRegistry.gauge("running_time_min", timer, t -> {
      System.out.println("measurements:");
      return Arrays.stream(timer.takeSnapshot().histogramCounts())
            .mapToDouble(CountAtBucket::bucket)
            .peek(System.out::println)
            .min().orElse(0);

//      System.out.println("measurements:");
//      return StreamSupport
//            .stream(measurements.spliterator(), false)
//            .mapToDouble(Measurement::getValue)
//            .peek(System.out::println)
//            .min().orElse(0);
    });
    return timer;
  }
}
