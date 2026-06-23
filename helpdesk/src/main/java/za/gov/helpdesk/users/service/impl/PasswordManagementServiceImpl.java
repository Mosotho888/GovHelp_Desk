package za.gov.helpdesk.users.service.impl;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.auth.service.RefreshTokenService;
import za.gov.helpdesk.users.dto.request.AdminPasswordResetRequest;
import za.gov.helpdesk.users.dto.request.ChangePasswordRequest;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;
import za.gov.helpdesk.users.service.PasswordManagementService;
import za.gov.helpdesk.users.service.UserQueryHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordManagementServiceImpl implements PasswordManagementService {

    private final UserRepository userRepository;
    private final UserQueryHelper userQuery;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuditEventPublisher auditPublisher;

    @Override
    @Transactional
    public void changeOwnPassword(final ChangePasswordRequest request, final User actor) {
        final User user = userQuery.findOrThrow(actor.getId());

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException("Cannot change password for an inactive account");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "New password must differ from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenService.revokeAll(user);

        auditPublisher.publishAudit(
                AuditLog.EntityType.USER,
                user.getId(),
                user,
                AuditLog.AuditAction.PASSWORD_RESET,
                null,
                null,
                "User changed their own password");

        log.info("Password changed by user={}", user.getEmail());
    }

    @Override
    @Transactional
    public void adminResetPassword(
            final Long targetUserId, final AdminPasswordResetRequest request, final User admin) {

        if (admin.getId().equals(targetUserId)) {
            throw new IllegalArgumentException(
                    "Admins must use the regular password change flow to update their "
                            + "own credentials");
        }

        final User target = userQuery.findOrThrow(targetUserId);

        target.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        target.setLoginAttempts(0);
        userRepository.save(target);

        refreshTokenService.revokeAll(target);

        final String reason =
                request.getReason() != null ? " - reason: " + request.getReason() : "";
        auditPublisher.publishAudit(
                AuditLog.EntityType.USER,
                target.getId(),
                admin,
                AuditLog.AuditAction.PASSWORD_RESET,
                null,
                null,
                "Password reset by admin: " + admin.getEmail() + reason);

        log.info("Password reset by admin={} for user={}", admin.getEmail(), target.getEmail());
    }
}
