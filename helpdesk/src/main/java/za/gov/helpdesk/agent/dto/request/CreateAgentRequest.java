package za.gov.helpdesk.agent.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import za.gov.helpdesk.agent.model.Agent;

@Data
public class CreateAgentRequest {

    @NotNull(message = "userId is required")
    private Long userId;
    private String department;
    private Agent.Availability availability = Agent.Availability.OFFLINE;
}
