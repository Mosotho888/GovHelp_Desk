package za.gov.helpdesk.unit.services.ticket;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.repository.jpa.AgentRepository;
import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.ticket.model.Status;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.ticket.service.impl.CategoryRoutingService;
import za.gov.helpdesk.users.model.User;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryRoutingService unit tests")
class CategoryRoutingServiceTest {

    @Mock private AgentRepository agentRepository;
    @Mock private TicketRepository ticketRepository;

    private CategoryRoutingService routingService;

    @BeforeEach
    void setUp() {
        routingService = new CategoryRoutingService(agentRepository, ticketRepository);
    }

    @Test
    @DisplayName("resolveAssignee() returns empty for a null category")
    void resolveAssignee_nullCategory_returnsEmpty() {
        assertThat(routingService.resolveAssignee(null)).isEmpty();
    }

    @Test
    @DisplayName("resolveAssignee() returns empty when the category has no default department")
    void resolveAssignee_noDefaultDepartment_returnsEmpty() {
        final Category category = Category.builder().id(1L).name("Misc").build();

        assertThat(routingService.resolveAssignee(category)).isEmpty();
    }

    @Test
    @DisplayName("resolveAssignee() returns empty when no agents are online in the department")
    void resolveAssignee_noOnlineAgents_returnsEmpty() {
        final Category category =
                Category.builder().id(1L).name("Hardware").defaultDepartment("IT Support").build();
        given(
                        agentRepository.findByDepartmentAndAvailability(
                                "IT Support", Agent.Availability.ONLINE))
                .willReturn(List.of());

        assertThat(routingService.resolveAssignee(category)).isEmpty();
    }

    @Test
    @DisplayName("resolveAssignee() picks the single available online agent")
    void resolveAssignee_singleOnlineAgent_returnsThatAgent() {
        final Category category =
                Category.builder().id(1L).name("Hardware").defaultDepartment("IT Support").build();
        final Agent agent = agent(10L);

        given(
                        agentRepository.findByDepartmentAndAvailability(
                                "IT Support", Agent.Availability.ONLINE))
                .willReturn(List.of(agent));
        lenient()
                .when(ticketRepository.countByAssigneeIdAndStatus(10L, Status.OPEN))
                .thenReturn(2L);
        lenient()
                .when(ticketRepository.countByAssigneeIdAndStatus(10L, Status.IN_PROGRESS))
                .thenReturn(1L);

        final Optional<Agent> result = routingService.resolveAssignee(category);

        assertThat(result).contains(agent);
    }

    @Test
    @DisplayName(
            "resolveAssignee() picks the agent with the lowest combined open + in-progress load")
    void resolveAssignee_multipleAgents_picksLeastLoaded() {
        final Category category =
                Category.builder().id(1L).name("Hardware").defaultDepartment("IT Support").build();
        final Agent busyAgent = agent(10L);
        final Agent idleAgent = agent(20L);

        given(
                        agentRepository.findByDepartmentAndAvailability(
                                "IT Support", Agent.Availability.ONLINE))
                .willReturn(List.of(busyAgent, idleAgent));
        given(ticketRepository.countByAssigneeIdAndStatus(10L, Status.OPEN)).willReturn(5L);
        given(ticketRepository.countByAssigneeIdAndStatus(10L, Status.IN_PROGRESS)).willReturn(3L);
        given(ticketRepository.countByAssigneeIdAndStatus(20L, Status.OPEN)).willReturn(1L);
        given(ticketRepository.countByAssigneeIdAndStatus(20L, Status.IN_PROGRESS)).willReturn(0L);

        final Optional<Agent> result = routingService.resolveAssignee(category);

        assertThat(result).contains(idleAgent);
    }

    @Test
    @DisplayName("resolveAssignee() sums both OPEN and IN_PROGRESS counts toward an agent's load")
    void resolveAssignee_combinesOpenAndInProgressCounts() {
        final Category category =
                Category.builder().id(1L).name("Hardware").defaultDepartment("IT Support").build();
        // agentA: 0 open + 4 in-progress = 4 total. agentB: 3 open + 0 in-progress = 3 total.
        // agentB should win despite agentA having zero OPEN tickets, proving both statuses count.
        final Agent agentA = agent(10L);
        final Agent agentB = agent(20L);

        given(
                        agentRepository.findByDepartmentAndAvailability(
                                "IT Support", Agent.Availability.ONLINE))
                .willReturn(List.of(agentA, agentB));
        given(ticketRepository.countByAssigneeIdAndStatus(10L, Status.OPEN)).willReturn(0L);
        given(ticketRepository.countByAssigneeIdAndStatus(10L, Status.IN_PROGRESS)).willReturn(4L);
        given(ticketRepository.countByAssigneeIdAndStatus(20L, Status.OPEN)).willReturn(3L);
        given(ticketRepository.countByAssigneeIdAndStatus(20L, Status.IN_PROGRESS)).willReturn(0L);

        final Optional<Agent> result = routingService.resolveAssignee(category);

        assertThat(result).contains(agentB);
    }

    private Agent agent(final long id) {
        final User user =
                User.builder().id(id).name("Agent " + id).email("a" + id + "@gov.za").build();
        return Agent.builder().id(id).user(user).availability(Agent.Availability.ONLINE).build();
    }
}
