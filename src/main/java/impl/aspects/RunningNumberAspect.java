package impl.aspects;

import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Aspect
@Component
public class RunningNumberAspect {
  AtomicInteger runningCount = new AtomicInteger(0);

  public RunningNumberAspect(MeterRegistry registry) {
    registry.gauge("running_scripts_number", runningCount);
  }

  @Around("@annotation(impl.aspects.annotations.Running)")
  public Object count(ProceedingJoinPoint joinPoint) throws Throwable {
    runningCount.incrementAndGet();
    Object proceed = joinPoint.proceed();
    runningCount.decrementAndGet();
    return proceed;
  }
}
