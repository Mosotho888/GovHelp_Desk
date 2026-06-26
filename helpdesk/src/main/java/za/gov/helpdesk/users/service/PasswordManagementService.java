package za.gov.helpdesk.users.service;

import za.gov.helpdesk.users.dto.request.AdminPasswordResetRequest;
import za.gov.helpdesk.users.dto.request.ChangePasswordRequest;
import za.gov.helpdesk.users.model.User;

public interface PasswordManagementService {

    void changeOwnPassword(ChangePasswordRequest request, User actor);

    void adminResetPassword(Long targetUserId, AdminPasswordResetRequest request, User admin);
}
