package za.gov.helpdesk.agent.dto.request;

import lombok.Data;
import za.gov.helpdesk.agent.model.Agent;

@Data
public class UpdateAgentRequest {

    private Agent.Availability availability;
    private String department;
}
