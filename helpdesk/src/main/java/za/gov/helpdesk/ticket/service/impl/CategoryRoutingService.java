package za.gov.helpdesk.ticket.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.repository.jpa.AgentRepository;
import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.ticket.model.Status;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;

import lombok.RequiredArgsConstructor;

/**
 * Resolves which agent an unassigned ticket should be automatically routed to, based on the
 * ticket's category. This is intentionally simple load-balancing rather than a full skills/rules
 * engine: pick the category's default department, then within it the {@code ONLINE} agent currently
 * carrying the fewest open-or-in-progress tickets. A ticket that can't be routed (no department
 * configured, or nobody online) is left unassigned so it still surfaces in the shared queue rather
 * than silently disappearing.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryRoutingService {

    private final AgentRepository agentRepository;
    private final TicketRepository ticketRepository;

    /**
     * Resolves an available {@link Agent} to assign to a given category based on current workload.
     *
     * <p>Finds all online agents belonging to the category's default department and selects the
     * agent with the lowest number of active (open or in-progress) assigned tickets.
     *
     * @param category the {@link Category} whose default department will be checked; may be {@code
     *     null}
     * @return an {@link Optional} containing the online agent with the lowest open workload, or
     *     {@link Optional#empty()} if the category or its default department is {@code null}, or if
     *     no online agents are available
     */
    public Optional<Agent> resolveAssignee(final Category category) {
        if (category == null || category.getDefaultDepartment() == null) {
            return Optional.empty();
        }

        final List<Agent> onlineAgents =
                agentRepository.findByDepartmentAndAvailability(
                        category.getDefaultDepartment(), Agent.Availability.ONLINE);

        return onlineAgents.stream().min(Comparator.comparingLong(this::currentOpenLoad));
    }

    /**
     * Calculates the total active ticket load for a given agent.
     *
     * @param agent the {@link Agent} whose open load is to be calculated
     * @return the sum of tickets currently assigned to the agent that are in {@code OPEN} or {@code
     *     IN_PROGRESS} status
     */
    private long currentOpenLoad(final Agent agent) {
        return ticketRepository.countByAssigneeIdAndStatus(agent.getId(), Status.OPEN)
                + ticketRepository.countByAssigneeIdAndStatus(agent.getId(), Status.IN_PROGRESS);
    }
}
