package za.gov.helpdesk.agent.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;

@Component
@Getter
public class AgentMetrics {

    private final Counter registered;
    private final Counter availabilityChanged;
    private final Counter departmentChanged;

    public AgentMetrics(final MeterRegistry registry) {

        this.registered =
                Counter.builder("helpdesk.agent.registered")
                        .description("Total agent created")
                        .register(registry);
        this.availabilityChanged =
                Counter.builder("helpdesk.agent.availability.changed")
                        .description("Agent availability status changes")
                        .register(registry);

        this.departmentChanged =
                Counter.builder("helpdesk.agent.department.changed")
                        .description("Agent department reassignments")
                        .register(registry);
    }

    public void incrementRegistered() {
        this.registered.increment();
    }

    public void incrementAvailabilityChanged() {
        this.availabilityChanged.increment();
    }

    public void incrementDepartmentChanged() {
        this.departmentChanged.increment();
    }
}
