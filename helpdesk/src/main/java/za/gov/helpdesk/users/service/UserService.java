package za.gov.helpdesk.users.service;

import org.springframework.data.domain.Page;
import za.gov.helpdesk.users.dto.*;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);
    UserResponse getUserById(Long id);
    Page<UserResponse> getAllUsers(Pageable pageable);
    UserResponse updateUser(Long id, UpdateUserRequest request);
    void deactivateUser(Long id);
}
