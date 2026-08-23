package za.gov.helpdesk.sla.metrics;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.model.Ticket.Priority;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;

@Component
@Getter
public class SlaMetrics {

    private final Map<Ticket.Priority, Counter> responseBreached = new EnumMap<>(Priority.class);
    private final Map<Priority, Counter> resolutionBreached = new EnumMap<>(Priority.class);
    private final Map<Priority, Counter> responseWarning = new EnumMap<>(Priority.class);
    private final Map<Priority, Counter> resolutionWarning = new EnumMap<>(Priority.class);

    public SlaMetrics(final MeterRegistry registry) {

        for (final Priority priority : Priority.values()) {

            responseBreached.put(
                    priority,
                    Counter.builder("helpdesk.sla.breach.response")
                            .description("Tickets that missed the first-response SLA deadline")
                            .tag("priority", priority.name())
                            .register(registry));

            resolutionBreached.put(
                    priority,
                    Counter.builder("helpdesk.sla.breach.resolution")
                            .description("Tickets that missed the resolution SLA deadline")
                            .tag("priority", priority.name())
                            .register(registry));

            responseWarning.put(
                    priority,
                    Counter.builder("helpdesk.sla.warning.response")
                            .description("First-response SLA warnings sent to agents")
                            .tag("priority", priority.name())
                            .register(registry));

            resolutionWarning.put(
                    priority,
                    Counter.builder("helpdesk.sla.warning.resolution")
                            .description("Resolution SLA warnings sent to agents")
                            .tag("priority", priority.name())
                            .register(registry));
        }
    }

    public void incrementResponseBreached(final Priority priority) {
        this.responseBreached.get(priority).increment();
    }

    public void incrementResolutionBreached(final Priority priority) {
        this.resolutionBreached.get(priority).increment();
    }

    public void incrementResponseWarning(final Priority priority) {
        this.responseWarning.get(priority).increment();
    }

    public void incrementResolutionWarning(final Priority priority) {
        this.resolutionWarning.get(priority).increment();
    }
}
