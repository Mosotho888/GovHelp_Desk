package za.gov.helpdesk.users.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import za.gov.helpdesk.users.dto.request.AdminPasswordResetRequest;
import za.gov.helpdesk.users.dto.request.ChangePasswordRequest;
import za.gov.helpdesk.users.dto.request.CreateUserRequest;
import za.gov.helpdesk.users.dto.request.UpdateUserRequest;
import za.gov.helpdesk.users.dto.response.UserResponse;
import za.gov.helpdesk.users.model.User;

public interface UserService {

    UserResponse createUser(CreateUserRequest request, User actor);

    UserResponse getUserById(Long id);

    UserResponse getUserByEmail(String email);

    Page<UserResponse> getAllUsers(Pageable pageable);

    UserResponse updateUser(Long id, UpdateUserRequest request, User actor);

    void deactivateUser(Long id, User actor);

    void adminResetPassword(Long targetUserId, AdminPasswordResetRequest request, User admin);

    void changeOwnPassword(ChangePasswordRequest request, User actor);

    void reactivateUser(Long id, User admin);

    UserResponse changeUserRole(Long id, User.Role newRole, User admin);
}
