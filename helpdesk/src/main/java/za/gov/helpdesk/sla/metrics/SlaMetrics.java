package za.gov.helpdesk.sla.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class SlaMetrics {

    private final Counter responseBreached;
    private final Counter resolutionBreached;
    private final Counter responseWarning;
    private final Counter resolutionWarning;

    public SlaMetrics(MeterRegistry registry) {

        this.responseBreached = Counter.builder("helpdesk.sla.breach.response")
                .description("Tickets that missed the first-response SLA deadline").register(registry);

        this.resolutionBreached = Counter.builder("helpdesk.sla.breach.resolution")
                .description("Tickets that missed the resolution SLA deadline").register(registry);

        this.responseWarning = Counter.builder("helpdesk.sla.warning.response")
                .description("First-response SLA warnings sent to agents").register(registry);

        this.resolutionWarning = Counter.builder("helpdesk.sla.warning.resolution")
                .description("Resolution SLA warnings sent to agents").register(registry);
    }

    public void incrementResponseBreached() {
        this.responseBreached.increment();
    }

    public void incrementResolutionBreached() {
        this.resolutionBreached.increment();
    }

    public void incrementResolutionWarning() {
        this.resolutionWarning.increment();
    }

    public void incrementResponseWarning() {
        this.responseWarning.increment();
    }
}
