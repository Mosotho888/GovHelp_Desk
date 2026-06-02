package za.gov.helpdesk.agent.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import za.gov.helpdesk.agent.dto.response.AgentResponse;
import za.gov.helpdesk.agent.dto.response.AgentStatsResponse;
import za.gov.helpdesk.agent.dto.request.CreateAgentRequest;
import za.gov.helpdesk.agent.dto.request.UpdateAgentRequest;
import za.gov.helpdesk.users.model.User;

public interface AgentService {

    AgentResponse createAgent(CreateAgentRequest request, User actor);
    AgentResponse getAgentById(Long id);
    Page<AgentResponse> getAllAgents(Pageable pageable);
    AgentStatsResponse getAgentStats(Long id);
    AgentResponse updateAgent(Long id, UpdateAgentRequest request, User actor);

}
