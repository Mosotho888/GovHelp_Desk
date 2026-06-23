package za.gov.helpdesk.agent.service;

import za.gov.helpdesk.users.model.User;

@FunctionalInterface
public interface AgentRoleLifecycleService {

    void handleRoleChange(User target, User.Role oldRole, User.Role newRole, User admin);
}
