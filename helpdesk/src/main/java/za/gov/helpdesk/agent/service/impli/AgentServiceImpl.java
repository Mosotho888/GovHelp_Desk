package za.gov.helpdesk.agent.service.impli;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.agent.dto.AgentResponse;
import za.gov.helpdesk.agent.dto.AgentStatsResponse;
import za.gov.helpdesk.agent.dto.CreateAgentRequest;
import za.gov.helpdesk.agent.dto.UpdateAgentRequest;
import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.repository.AgentRepository;
import za.gov.helpdesk.agent.repository.jdbc.ReportJdbcRepository;
import za.gov.helpdesk.agent.service.AgentService;
import za.gov.helpdesk.users.exception.UserAlreadyExistsException;
import za.gov.helpdesk.users.exception.UserNotFoundException;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    private final ReportJdbcRepository reportJdbcRepository;

    @Override
    @Transactional
    public AgentResponse createAgent(CreateAgentRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(UserNotFoundException::new);

        if (agentRepository.existsByUserId(request.getUserId())) {
            throw new UserAlreadyExistsException();
        }

        // Promote user role to AGENT if not already ADMIN
        if (user.getRole() == User.Role.USER) {
            user.setRole(User.Role.AGENT);
            userRepository.save(user);
        }

        Agent agent = Agent.builder()
                .user(user)
                .department(request.getDepartment())
                .availability(request.getAvailability() != null ? request.getAvailability() : Agent.Availability.OFFLINE)
                .build();

        return toResponse(agentRepository.save(agent));
    }

    @Override
    @Transactional(readOnly = true)
    public AgentResponse getAgentById(Long id) {

        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AgentResponse> getAllAgents(Pageable pageable) {

        return agentRepository.findAll(pageable).map(agent -> toResponse(agent));
    }

    @Override
    @Transactional(readOnly = true)
    public AgentStatsResponse getAgentStats(Long id) {
        Agent agent = findOrThrow(id);
        Map<String, Object> raw = reportJdbcRepository.getAgentsStats(id);

        return AgentStatsResponse.builder()
                .agentId(id)
                .agentName(agent.getUser().getName())
                .totalAssigned(toLong(raw.get("total_assigned")))
                .openCount(toLong(raw.get("open_count")))
                .inProgressCount(toLong(raw.get("in_progress_count")))
                .resolvedCount(toCount(raw.get("resolved_count")))
                .closedCount(toLong(raw.get("closed_count")))
                .escalatedCount(toLong(raw.get("escalated_count")))
                .avgResolutionHours(toDouble(raw.get("avg_resolution_hours")))
                .build();
    }

    @Override
    @Transactional
    public AgentResponse updateAgent(UpdateAgentRequest request) {
        Agent agent = findOrThrow(id);

        if (request.getAvailability() != null) {
            agent.setAvailability(request.getAvailability());
        }
        if (request.getDepartment() != null) {
            agent.setDepartment(request.getDepartment());
        }
        return toResponse(agentRepository.save(agent));
    }
}
