package za.gov.helpdesk.agent.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import za.gov.helpdesk.agent.dto.AgentResponse;
import za.gov.helpdesk.agent.dto.AgentStatsResponse;
import za.gov.helpdesk.agent.dto.CreateAgentRequest;
import za.gov.helpdesk.agent.dto.UpdateAgentRequest;

public interface AgentService {

    AgentResponse createAgent(CreateAgentRequest request);
    AgentResponse getAgentById(Long id);
    Page<AgentResponse> getAllAgents(Pageable pageable);
    AgentStatsResponse getAgentStats(Long id);
    AgentResponse updateAgent(Long id, UpdateAgentRequest request);

}
