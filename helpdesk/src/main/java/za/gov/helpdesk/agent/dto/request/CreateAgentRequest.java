package za.gov.helpdesk.agent.dto.request;

import jakarta.validation.constraints.NotNull;

import za.gov.helpdesk.agent.model.Agent;

import lombok.Data;

@Data
public class CreateAgentRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    private String department;
    private Agent.Availability availability = Agent.Availability.OFFLINE;
}
