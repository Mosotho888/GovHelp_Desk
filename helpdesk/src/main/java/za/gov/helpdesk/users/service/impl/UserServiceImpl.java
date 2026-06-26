package za.gov.helpdesk.users.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.exception.DuplicateResourceException;
import za.gov.helpdesk.users.dto.request.ChangePasswordRequest;
import za.gov.helpdesk.users.dto.request.CreateUserRequest;
import za.gov.helpdesk.users.dto.request.UpdateUserRequest;
import za.gov.helpdesk.users.dto.response.UserResponse;
import za.gov.helpdesk.users.mapper.UserMapper;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;
import za.gov.helpdesk.users.service.PasswordManagementService;
import za.gov.helpdesk.users.service.UserQueryHelper;
import za.gov.helpdesk.users.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserQueryHelper userQuery;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventPublisher auditPublisher;
    private final PasswordManagementService passwordManagementService;

    @Override
    @Transactional
    public UserResponse createUser(final CreateUserRequest request, final User actor) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "A user with '" + request.getEmail() + "' already exists");
        }

        final User user =
                User.builder()
                        .name(request.getName())
                        .email(request.getEmail().toLowerCase().trim())
                        .passwordHash(passwordEncoder.encode(request.getPassword()))
                        .role(request.getRole() != null ? request.getRole() : User.Role.USER)
                        .phone(request.getPhone())
                        .timezone(
                                request.getTimezone() != null
                                        ? request.getTimezone()
                                        : "Africa/Johannesburg")
                        .active(true)
                        .build();

        final User savedUser;

        try {
            savedUser = userRepository.save(user);
        } catch (final DataIntegrityViolationException e) {
            log.error("Data integrity violation while registering email={}", request.getEmail(), e);
            throw new DuplicateResourceException(
                    "A user with email '" + request.getEmail() + "' was simultaneously registered");
        }

        auditPublisher.publishAudit(
                AuditLog.EntityType.USER,
                savedUser.getId(),
                actor,
                AuditLog.AuditAction.USER_CREATED,
                null,
                savedUser.getEmail(),
                "User created with role " + savedUser.getRole().name());

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(final Long id) {
        return userMapper.toUserResponse(userQuery.findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(final String email) {

        return userMapper.toUserResponse(userQuery.findByEmailOrThrow(email));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(final Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toUserResponse);
    }

    @Override
    @Transactional
    public UserResponse updateUser(
            final Long id, final UpdateUserRequest request, final User actor) {

        final User user = userQuery.findOrThrow(id);
        final List<String> changes = new ArrayList<>();

        if (request.getName() != null && !request.getName().equals(user.getName())) {
            changes.add(String.format("name: '%s' -> '%s'", user.getName(), request.getName()));
            user.setName(request.getName());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
            changes.add(String.format("phone: '%s' -> '%s'", user.getPhone(), request.getPhone()));
        }

        if (request.getTimezone() != null) {
            user.setTimezone(request.getTimezone());
            changes.add(
                    String.format(
                            "timezone: '%s' -> '%s'", user.getTimezone(), request.getTimezone()));
        }

        final User savedUser = saveUser(user);

        if (!changes.isEmpty()) {
            final String auditDescription = "Updated: " + String.join("; ", changes);
            auditPublisher.publishAudit(
                    AuditLog.EntityType.USER,
                    savedUser.getId(),
                    actor,
                    AuditLog.AuditAction.USER_UPDATED,
                    null,
                    null,
                    auditDescription);
        }

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public User saveUser(final User user) {

        return userRepository.save(user);
    }

    @Override
    public void changeOwnPassword(final ChangePasswordRequest request, final User actor) {
        passwordManagementService.changeOwnPassword(request, actor);
    }
}
