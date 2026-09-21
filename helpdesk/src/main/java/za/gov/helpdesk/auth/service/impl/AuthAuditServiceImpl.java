package za.gov.helpdesk.auth.service.impl;

import org.springframework.stereotype.Service;

import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.auth.service.AuthAuditService;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthAuditServiceImpl implements AuthAuditService {

    private final AuditEventPublisher auditPublisher;

    @Override
    public void loggedInSuccessful(final User user) {

        auditPublisher.publishAuthAudit(
                AuditLog.AuditAction.LOGIN_SUCCESS,
                user.getId(),
                user.getName(),
                user.getRole().name(),
                "Login successful");
    }

    @Override
    public void loggedOut(final User actor) {

        auditPublisher.publishAuthAudit(
                AuditLog.AuditAction.FORCED_LOGOUT,
                actor.getId(),
                actor.getName(),
                actor.getRole().name(),
                "User logged out");
    }

    @Override
    public void passwordReset(final User user) {

        auditPublisher.publishAuthAudit(
                AuditLog.AuditAction.PASSWORD_RESET,
                user.getId(),
                user.getName(),
                user.getRole().name(),
                "Password reset via OTP");
    }

    @Override
    public void accountLocked(final User user, final int attempts) {

        auditPublisher.publishAuthAudit(
                AuditLog.AuditAction.ACCOUNT_LOCKED,
                user.getId(),
                user.getName(),
                user.getRole().name(),
                attempts + " consecutive failed login attempts");
    }
}
