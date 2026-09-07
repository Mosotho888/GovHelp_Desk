package za.gov.helpdesk.agent.service;

import za.gov.helpdesk.users.model.Role;
import za.gov.helpdesk.users.model.User;

@FunctionalInterface
public interface AgentRoleLifecycleService {

    void handleRoleChange(User target, Role oldRole, Role newRole, User admin);
}
