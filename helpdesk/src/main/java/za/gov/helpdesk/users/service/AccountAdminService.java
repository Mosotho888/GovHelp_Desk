package za.gov.helpdesk.users.service;

import za.gov.helpdesk.users.dto.response.UserResponse;
import za.gov.helpdesk.users.model.User;

public interface AccountAdminService {

    void deactivateUser(Long id, User admin);
    void reactivateUser(Long id, User admin);
    UserResponse changeUserRole(Long id, User.Role newRole, User admin);
}
