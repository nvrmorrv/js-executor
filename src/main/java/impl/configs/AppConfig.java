package impl.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.CollectionFactory;
import org.springframework.data.keyvalue.core.KeyValueAdapter;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.core.KeyValueTemplate;
import org.springframework.data.map.MapKeyValueAdapter;
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
  public TimedAspect timedAspect(MeterRegistry registry) {
    return new TimedAspect(registry);
  }

  @Bean
  public KeyValueOperations keyValueTemplate(KeyValueAdapter adapter) {
    return new KeyValueTemplate(adapter);
  }

  @Bean
  public KeyValueAdapter keyValueAdapter(Map<String, Map<Object, Object>> scriptRepositoryStore) {
    return new MapKeyValueAdapter(scriptRepositoryStore);
  }

  @Bean(name = "scriptRepositoryStore")
  public Map<String, Map<Object, Object>> scriptRepositoryStore() {
    return CollectionFactory.createMap(ConcurrentHashMap.class, 100);
  }

  @PostConstruct
  public void setMapSizeMetric(MeterRegistry registry, Map<String, Map<Object, Object>> scriptRepositoryStore) {
    registry.gaugeMapSize("map_size", Tags.empty(), scriptRepositoryStore);
  }


}
