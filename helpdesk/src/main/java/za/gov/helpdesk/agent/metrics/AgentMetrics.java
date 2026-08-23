package za.gov.helpdesk.agent.metrics;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.repository.jpa.AgentRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;

@Component
@Getter
public class AgentMetrics {

    private final Counter registered;
    private final Counter departmentChanged;
    private final Map<Agent.Availability, Counter> availabilityChanged =
            new EnumMap<>(Agent.Availability.class);

    private final AgentRepository agentRepository;
    private final AtomicInteger activeAgents = new AtomicInteger(0);

    public AgentMetrics(final MeterRegistry registry, final AgentRepository agentRepository) {

        this.agentRepository = agentRepository;

        this.registered =
                Counter.builder("helpdesk.agent.registered")
                        .description("Total agents created")
                        .register(registry);

        this.departmentChanged =
                Counter.builder("helpdesk.agent.department.changed")
                        .description("Agent department reassignments")
                        .register(registry);

        for (final Agent.Availability availability : Agent.Availability.values()) {
            availabilityChanged.put(
                    availability,
                    Counter.builder("helpdesk.agent.availability.changed")
                            .description("Agent availability status changes")
                            .tag("availability", availability.name())
                            .register(registry));
        }

        Gauge.builder("helpdesk.agent.active", activeAgents, AtomicInteger::get)
                .description("Current count of agents with ONLINE availability")
                .register(registry);
    }

    public void incrementRegistered() {
        this.registered.increment();
    }

    public void incrementAvailabilityChanged(final Agent.Availability newAvailability) {
        this.availabilityChanged.get(newAvailability).increment();
    }

    public void incrementDepartmentChanged() {
        this.departmentChanged.increment();
    }

    @Scheduled(fixedRate = 30000)
    public void refreshActiveAgentGauge() {
        activeAgents.set((int) agentRepository.countByAvailability(Agent.Availability.ONLINE));
    }
}
