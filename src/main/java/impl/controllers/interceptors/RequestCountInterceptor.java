package impl.controllers.interceptors;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@AllArgsConstructor
@Slf4j
public class RequestCountInterceptor implements HandlerInterceptor {
  private final MeterRegistry meterRegistry;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    meterRegistry.counter("request_count").increment();
    return true;
  }

}
