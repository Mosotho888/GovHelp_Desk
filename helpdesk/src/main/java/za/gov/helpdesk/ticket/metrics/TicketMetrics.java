package za.gov.helpdesk.ticket.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@Getter
public class TicketMetrics {

    private final Counter created;
    private final Counter resolved;
    private final Counter closed;
    private final Counter escalated;

    private final Timer resolutionTime;

    public TicketMetrics(MeterRegistry registry) {

        this.created = Counter.builder("helpdesk.ticket.created")
                .description("Total support tickets created")
                .register(registry);

        this.resolved = Counter.builder("helpdesk.ticket.resolved")
                .description("Total support tickets transitioned to RESOLVED")
                .register(registry);

        this.closed = Counter.builder("helpdesk.ticket.closed")
                .description("Total support tickets transitioned to CLOSED")
                .register(registry);

        this.escalated = Counter.builder("helpdesk.ticket.escalated")
                .description("Total support tickets escalated")
                .register(registry);

        this.resolutionTime = Timer.builder("helpdesk.ticket.resolution.time")
                .description("Elapsed time from ticket creation to first RESOLVED transition")
                .publishPercentileHistogram()
                .register(registry);
    }

    public void incrementCreated() {
        this.created.increment();
    }
    public void incrementResolved() {
        this.resolved.increment();
    }
    public void incrementClosed() {
        this.closed.increment();
    }
    public void incrementEscalated() {
        this.escalated.increment();
    }
    public void recordResolutionTime(LocalDateTime ticketCreatedAt) {
        this.resolutionTime.record(Duration.between(ticketCreatedAt, LocalDateTime.now()));
    }
}
