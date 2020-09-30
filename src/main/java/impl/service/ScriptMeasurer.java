package impl.service;

import impl.repositories.ScriptRepository;
import impl.repositories.entities.Script;
import impl.shared.ScriptStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScriptMeasurer {
  private final ScriptRepository repo;
  private final AtomicInteger queueGauge = new AtomicInteger(0);
  private final AtomicInteger runningGauge = new AtomicInteger(0);
  private final AtomicInteger cancelledGauge = new AtomicInteger(0);
  private final AtomicInteger doneGauge = new AtomicInteger(0);
  private final AtomicInteger doneWithExceptionGauge = new AtomicInteger(0);

  public ScriptMeasurer(MeterRegistry meterRegistry, ScriptRepository repo) {
    this.repo = repo;
    String metricName = "script_status_gauge";
    meterRegistry.gauge(metricName, Tags.of("status", ScriptStatus.QUEUE.name()), queueGauge);
    meterRegistry.gauge(metricName, Tags.of("status", ScriptStatus.RUNNING.name()), runningGauge);
    meterRegistry.gauge(metricName, Tags.of("status", ScriptStatus.CANCELLED.name()), cancelledGauge);
    meterRegistry.gauge(metricName, Tags.of("status", ScriptStatus.DONE.name()), doneGauge);
    meterRegistry.gauge(metricName, Tags.of("status", ScriptStatus.DONE_WITH_EXCEPTION.name()), doneWithExceptionGauge);
  }


  @Scheduled(fixedRate = 200)
  public void countScriptByStatus() {
    int queueNum = 0, runningNum = 0, cancelledNum = 0, doneNum = 0, doneWithExceptionNum = 0;
    List<Script> scripts = repo.getScripts();
    for (Script script : scripts) {
      switch (script.getStatus()) {
        case QUEUE:
          queueNum++;
          break;
        case RUNNING:
          runningNum++;
          break;
        case CANCELLED:
          cancelledNum++;
          break;
        case DONE:
          doneNum++;
          break;
        case DONE_WITH_EXCEPTION:
          doneWithExceptionNum++;
          break;
      }
    }
    queueGauge.set(queueNum);
    runningGauge.set(runningNum);
    cancelledGauge.set(cancelledNum);
    doneGauge.set(doneNum);
    doneWithExceptionGauge.set(doneWithExceptionNum);
  }
}
