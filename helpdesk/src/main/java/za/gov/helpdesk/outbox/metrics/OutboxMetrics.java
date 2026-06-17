package za.gov.helpdesk.outbox.metrics;

import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class OutboxMetrics {

    private final Counter published;
    private final Counter failed;
    private final Counter deadLetter;

    private final AtomicLong pendingGauge = new AtomicLong(0);

    public OutboxMetrics(MeterRegistry registry) {

        this.published = Counter.builder("helpdesk.outbox.published")
                .description("Outbox events successfully published to RabbitMQ").register(registry);

        this.failed = Counter.builder("helpdesk.outbox.failed")
                .description("Outbox publish attempts that resulted in an exception (will retry)").register(registry);

        this.deadLetter = Counter.builder("helpdesk.outbox.dead.letter")
                .description("Outbox events that exhausted all retries and are permanently stuck").register(registry);

        Gauge.builder("helpdesk.outbox.pending.events", pendingGauge, AtomicLong::get)
                .description("Current number of PENDING events waiting in the outbox table").register(registry);
    }

    public void setPendingGauge(long pending) {
        this.pendingGauge.set(pending);
    }

    public void incrementFailed() {
        this.failed.increment();
    }

    public void incrementDeadLetter() {
        this.deadLetter.increment();
    }

    public void incrementPublished() {
        this.published.increment();
    }

}
