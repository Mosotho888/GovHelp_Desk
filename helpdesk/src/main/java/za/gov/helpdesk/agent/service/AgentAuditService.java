package za.gov.helpdesk.agent.service;

import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.users.model.User;

public interface AgentAuditService {

    void agentCreated(Agent savedAgent, User actor, User user);

    void agentUpdatedAvailability(Agent agent, User actor, String newAvailability);

    void agentUpdatedDepartment(Agent agent, User actor, String newDepartment);
}
