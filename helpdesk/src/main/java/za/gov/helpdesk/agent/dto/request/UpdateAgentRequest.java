package za.gov.helpdesk.agent.dto.request;

import za.gov.helpdesk.agent.model.Agent;

import lombok.Data;

@Data
public class UpdateAgentRequest {

    private Agent.Availability availability;
    private String department;
}
