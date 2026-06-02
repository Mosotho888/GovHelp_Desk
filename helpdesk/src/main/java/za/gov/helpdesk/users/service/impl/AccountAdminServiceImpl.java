package za.gov.helpdesk.users.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.auth.service.RefreshTokenService;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.users.converter.UserMapper;
import za.gov.helpdesk.users.dto.response.UserResponse;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;
import za.gov.helpdesk.users.service.AccountAdminService;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountAdminServiceImpl implements AccountAdminService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;
    private final AuditEventPublisher auditPublisher;

    @Override
    @Transactional
    public void deactivateUser(Long id, User admin) {
        User user = findOrThrow(id);
        user.setActive(false);
        userRepository.save(user);

        auditPublisher.publishAudit(
                AuditLog.EntityType.USER, user.getId(), admin,
                AuditLog.AuditAction.USER_DEACTIVATED,
                "active", "inactive",
                "Deactivated by " + admin.getName()
        );

        log.info("User deactivated: user={} by admin={}", user.getEmail(), admin.getEmail());
    }

    @Override
    @Transactional
    public void reactivateUser(Long id, User admin) {
        User target = findOrThrow(id);

        if (Boolean.TRUE.equals(target.getActive())) {
            throw new IllegalStateException("User account is already active");
        }

        target.setActive(true);
        target.setLoginAttempts(0);
        userRepository.save(target);

        auditPublisher.publishAudit(
                AuditLog.EntityType.USER, target.getId(), admin,
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
        if (target.getId().equals(admin.getId())) {
            throw new IllegalStateException("Admin cannot change their own role");
        }

        User.Role oldRole = target.getRole();
        target.setRole(newRole);
        userRepository.save(target);

        // Role is embedded in the JWT — revoke all sessions to force re-login
        refreshTokenService.revokeAll(target);

        auditPublisher.publishAudit(
                AuditLog.EntityType.USER, target.getId(), admin,
                AuditLog.AuditAction.ROLE_CHANGED,
                oldRole.name(), newRole.name(),
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
