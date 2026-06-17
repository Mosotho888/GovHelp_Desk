package za.gov.helpdesk.users.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.auth.service.RefreshTokenService;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.users.dto.request.AdminPasswordResetRequest;
import za.gov.helpdesk.users.dto.request.ChangePasswordRequest;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;
import za.gov.helpdesk.users.service.PasswordManagementService;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordManagementServiceImpl implements PasswordManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuditEventPublisher auditPublisher;

    @Override
    @Transactional
    public void changeOwnPassword(ChangePasswordRequest request, User actor) {
        User user = findOrThrow(actor.getId());

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must differ from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenService.revokeAll(user);

        auditPublisher.publishAudit(AuditLog.EntityType.USER, user.getId(), user, AuditLog.AuditAction.PASSWORD_RESET,
                null, null, "User changed their own password");

        log.info("Password changed by user={}", user.getEmail());
    }

    @Override
    @Transactional
    public void adminResetPassword(Long targetUserId, AdminPasswordResetRequest request, User admin) {
        User target = findOrThrow(targetUserId);

        target.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        target.setLoginAttempts(0);
        userRepository.save(target);

        refreshTokenService.revokeAll(target);

        String reason = request.getReason() != null ? " — reason: " + request.getReason() : "";
        auditPublisher.publishAudit(AuditLog.EntityType.USER, target.getId(), admin,
                AuditLog.AuditAction.PASSWORD_RESET, null, null,
                "Password reset by admin: " + admin.getEmail() + reason);

        log.info("Password reset by admin={} for user={}", admin.getEmail(), target.getEmail());
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
