package za.gov.helpdesk.agent.dto.response;

import lombok.Builder;
import lombok.Data;
import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.users.dto.response.UserResponse;

@Data
@Builder
public class AgentResponse {

    private Long id;
    private UserResponse user;
    private String department;
    private Agent.Availability availability;
}
