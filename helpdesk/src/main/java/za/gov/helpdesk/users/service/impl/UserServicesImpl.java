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
import za.gov.helpdesk.users.service.UserService;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServicesImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventPublisher auditPublisher;
    private final RefreshTokenService refreshTokenService;


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
    @Transactional
    public void deactivateUser(Long id, User actor) {
        User user = findOrThrow(id);

        user.setActive(false);
        userRepository.save(user);

        auditPublisher.publishAudit(
                AuditLog.EntityType.USER,
                user.getId(),
                actor,
                AuditLog.AuditAction.USER_DEACTIVATED,
                "active",
                "inactive",
                "Deactivated by " + actor.getName()
        );
    }

    @Override
    @Transactional
    public void adminResetPassword(Long targetUserId,
                                   AdminPasswordResetRequest request,
                                   User admin) {

        User target = findOrThrow(targetUserId);

        target.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        target.setLoginAttempts(0);          // clear any lockout state
        userRepository.save(target);

        // Revoke all active sessions — forces re-login
        refreshTokenService.revokeAll(target);

        auditPublisher.publishAudit(
                AuditLog.EntityType.USER,
                target.getId(),
                admin,
                AuditLog.AuditAction.PASSWORD_RESET,
                null, null,
                "Password reset by admin: " + admin.getEmail()
                        + (request.getReason() != null ? " — reason: " + request.getReason() : "")
        );

        log.info("Password reset by admin={} for user={}", admin.getEmail(), target.getEmail());
    }

    @Override
    @Transactional
    public void changeOwnPassword(ChangePasswordRequest request, User actor) {

        User user = findOrThrow(actor.getId());

        // Must verify current password first
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Prevent reuse of the same password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must differ from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens — forces re-login on other devices
        refreshTokenService.revokeAll(user);

        auditPublisher.publishAudit(
                AuditLog.EntityType.USER,
                user.getId(),
                user,
                AuditLog.AuditAction.PASSWORD_RESET,
                null, null,
                "User changed their own password"
        );

        log.info("Password changed by user={}", user.getEmail());
    }

    @Override
    @Transactional
    public void reactivateUser(Long id, User admin) {

        User target = findOrThrow(id);

        if (target.getActive()) {
            throw new IllegalStateException("User account is already active");
        }

        target.setActive(true);
        target.setLoginAttempts(0);
        userRepository.save(target);

        auditPublisher.publishAudit(
                AuditLog.EntityType.USER,
                target.getId(),
                admin,
                AuditLog.AuditAction.USER_REACTIVATED,
                "inactive", "active",
                "Reactivated by admin: " + admin.getEmail()
        );

        log.info("User reactivated: user={} by admin={}", target.getEmail(), admin.getEmail());
    }

    @Override
    @Transactional
    public UserResponse changeUserRole(Long id, User.Role newRole, User admin) {

        User target = findOrThrow(id);

        if (target.getRole() == newRole) {
            throw new IllegalStateException("User already has role " + newRole);
        }

        // Prevent admin from demoting themselves
        if (target.getId().equals(admin.getId())) {
            throw new IllegalStateException("Admin cannot change their own role");
        }

        User.Role oldRole = target.getRole();
        target.setRole(newRole);
        userRepository.save(target);

        // Role change invalidates all sessions - role is embedded in JWT
        refreshTokenService.revokeAll(target);

        auditPublisher.publishAudit(
                AuditLog.EntityType.USER,
                target.getId(),
                admin,
                AuditLog.AuditAction.ROLE_CHANGED,
                oldRole.name(),
                newRole.name(),
                "Role changed by admin: " + admin.getEmail()
        );

        log.info("Role changed: user={} {} -> {} by admin={}",
                target.getEmail(), oldRole, newRole, admin.getEmail());

        return userMapper.toUserResponse(target);
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
