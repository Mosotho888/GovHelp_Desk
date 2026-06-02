package za.gov.helpdesk.users.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.repository.jpa.AgentRepository;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.auth.service.RefreshTokenService;
import za.gov.helpdesk.exception.DuplicateResourceException;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.users.converter.UserMapper;
import za.gov.helpdesk.users.dto.request.AdminPasswordResetRequest;
import za.gov.helpdesk.users.dto.request.ChangePasswordRequest;
import za.gov.helpdesk.users.dto.request.CreateUserRequest;
import za.gov.helpdesk.users.dto.request.UpdateUserRequest;
import za.gov.helpdesk.users.dto.response.UserResponse;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import za.gov.helpdesk.users.service.AccountAdminService;
import za.gov.helpdesk.users.service.PasswordManagementService;
import za.gov.helpdesk.users.service.UserService;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServicesImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventPublisher auditPublisher;
    private final PasswordManagementService passwordManagementService;
    private final AccountAdminService accountAdminService;


    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request, User actor) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "A user with '" + request.getEmail() + "' already exists"
            );
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

        User savedUser = userRepository.save(user);

        auditPublisher.publishAudit(
                AuditLog.EntityType.USER,
                savedUser.getId(),
                actor,
                AuditLog.AuditAction.USER_CREATED,
                null,
                savedUser.getEmail(),
                "User created with role " + savedUser.getRole().name()
        );

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userMapper.toUserResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toUserResponse);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request, User actor) {

        User user = findOrThrow(id);

        StringBuilder changes = new StringBuilder();

        if (request.getName() != null && !request.getName().equals(user.getName())) {
            changes.append("name: ").append(user.getName())
                    .append(" -> ").append(request.getName());
            user.setName(request.getName());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
            changes.append("phone updated; ");
        }

        if (request.getTimezone() != null) {
            user.setTimezone(request.getTimezone());
            changes.append("timezone: ").append(request.getTimezone()).append("; ");
        }

        User savedUser = userRepository.save(user);

        if (!changes.isEmpty()) {
            auditPublisher.publishAudit(
                    AuditLog.EntityType.USER,
                    savedUser.getId(),
                    actor,
                    AuditLog.AuditAction.USER_UPDATED,
                    null,
                    null,
                    changes.toString().trim()
            );
        }

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    public void deactivateUser(Long id, User actor) {
        accountAdminService.deactivateUser(id, actor);
    }

    @Override
    public void reactivateUser(Long id, User admin) {
        accountAdminService.reactivateUser(id, admin);
    }

    @Override
    public UserResponse changeUserRole(Long id, User.Role newRole, User admin) {
        return accountAdminService.changeUserRole(id, newRole, admin);
    }

    @Override
    public void changeOwnPassword(ChangePasswordRequest request, User actor) {
        passwordManagementService.changeOwnPassword(request, actor);
    }

    @Override
    public void adminResetPassword(Long targetUserId,
                                   AdminPasswordResetRequest request,
                                   User admin) {
        passwordManagementService.adminResetPassword(targetUserId, request, admin);
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
