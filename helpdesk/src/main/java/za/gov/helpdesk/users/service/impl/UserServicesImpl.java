package za.gov.helpdesk.users.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.users.dto.*;
import za.gov.helpdesk.common.util.EmployeeUtil;
import za.gov.helpdesk.common.util.TicketUtil;
import za.gov.helpdesk.users.exception.UserAlreadyExistsException;
import za.gov.helpdesk.users.exception.UserNotFoundException;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import za.gov.helpdesk.users.service.UserService;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServicesImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException();
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : User.Role.USER)
                .phone(request.getPhone())
                .timezone(request.getTimezone() != null ? request.getTimezone() : "Africa/Johannesburg")
                .active(true)
                .build();

        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(user -> toResponse(user));
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {

        User user = findOrThrow(id);

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (request.getTimezone() != null) {
            user.setTimezone(request.getTimezone());
        }

        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deactivateUser(Long id) {
        User user = findOrThrow(id);
        user.setActive(false);
        userRepository.save(user);
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .role(user.getRole())
                .phone(user.getPhone())
                .timezone(user.getTimezone())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
