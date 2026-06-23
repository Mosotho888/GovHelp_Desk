package za.gov.helpdesk.agent.dto.response;

import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.users.dto.response.UserResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentResponse {

    private Long id;
    private UserResponse user;
    private String department;
    private Agent.Availability availability;
}
