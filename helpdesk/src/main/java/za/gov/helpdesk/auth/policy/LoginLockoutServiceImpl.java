package za.gov.helpdesk.auth.policy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.auth.service.AuthAuditService;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginLockoutServiceImpl implements LoginLockoutService {

    private final UserRepository userRepository;
    private final AuthAuditService authAuditService;

    @Value("${app.security.max-login-attempts}")
    private int maxLoginAttempts;

    @Override
    @Transactional
    public void recordFailedAttempt(final String email) {

        userRepository
                .findByEmail(email)
                .ifPresent(
                        user -> {
                            final int attempts = user.getLoginAttempts() + 1;
                            user.setLoginAttempts(attempts);

                            if (attempts >= maxLoginAttempts) {
                                user.setActive(false);

                                authAuditService.accountLocked(user, attempts);

                                log.warn(
                                        "Account locked after {} failed attempts: email={}",
                                        attempts,
                                        email);
                            }

                            userRepository.save(user);
                        });
    }

    @Override
    @Transactional
    public void resetFailedAttempts(final User user) {
        if (user.getLoginAttempts() > 0) {
            user.setLoginAttempts(0);
            userRepository.save(user);
        }
    }
}
