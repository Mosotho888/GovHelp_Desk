package za.gov.helpdesk.auth.policy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.users.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginLockoutServiceImpl implements LoginLockoutService {

    private final UserRepository userRepository;
    private final AuditEventPublisher auditPublisher;

    @Value("${app.security.max-login-attempts}")
    private int maxLoginAttempts;

    @Override
    @Transactional
    public void recordFailedAttempt(String email) {

        userRepository.findByEmail(email).ifPresent(user -> {
            int attempts = user.getLoginAttempts() + 1;
            user.setLoginAttempts(attempts);

            if (attempts >= maxLoginAttempts) {
                user.setActive(false);

                auditPublisher.publishAuthAudit(AuditLog.AuditAction.ACCOUNT_LOCKED, user.getId(), user.getName(),
                        user.getRole().name(), attempts + " consecutive failed login attempts");

                log.warn("Account locked after {} failed attempts: email={}", attempts, email);
            }

            userRepository.save(user);
        });
    }
}
