package za.gov.helpdesk.users.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    User saveUser(User user);

    void changeOwnPassword(ChangePasswordRequest request, User actor);
}
