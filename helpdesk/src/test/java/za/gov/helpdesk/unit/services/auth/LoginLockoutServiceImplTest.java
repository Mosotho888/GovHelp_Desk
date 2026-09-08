package za.gov.helpdesk.unit.services.auth;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.auth.policy.LoginLockoutServiceImpl;
import za.gov.helpdesk.users.model.Role;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginLockoutServiceImpl unit tests")
class LoginLockoutServiceImplTest {

    private static final int MAX_ATTEMPTS = 5;

    @Mock private UserRepository userRepository;
    @Mock private AuditEventPublisher auditPublisher;

    private LoginLockoutServiceImpl service;

    private User user;

    @BeforeEach
    void setUp() {
        service = new LoginLockoutServiceImpl(userRepository, auditPublisher);
        ReflectionTestUtils.setField(service, "maxLoginAttempts", MAX_ATTEMPTS);

        user =
                User.builder()
                        .id(1L)
                        .name("John Public")
                        .email("john@citizen.za")
                        .role(Role.USER)
                        .active(true)
                        .loginAttempts(0)
                        .build();
    }

    @Test
    @DisplayName("recordFailedAttempt() increments attempts without locking below the threshold")
    void recordFailedAttempt_belowThreshold_incrementsOnly() {
        user.setLoginAttempts(MAX_ATTEMPTS - 2);
        given(userRepository.findByEmail("john@citizen.za")).willReturn(Optional.of(user));

        service.recordFailedAttempt("john@citizen.za");

        assertThat(user.getLoginAttempts()).isEqualTo(MAX_ATTEMPTS - 1);
        assertThat(user.getActive()).isTrue();
        then(auditPublisher).should(never()).publishAuthAudit(any(), any(), any(), any(), any());
        then(userRepository).should(times(1)).save(user);
    }

    @Test
    @DisplayName("recordFailedAttempt() locks the account once attempts reach the threshold")
    void recordFailedAttempt_reachesThreshold_locksAccount() {
        user.setLoginAttempts(MAX_ATTEMPTS - 1);
        given(userRepository.findByEmail("john@citizen.za")).willReturn(Optional.of(user));

        service.recordFailedAttempt("john@citizen.za");

        assertThat(user.getLoginAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(user.getActive()).isFalse();
        then(auditPublisher)
                .should(times(1))
                .publishAuthAudit(
                        eq(AuditLog.AuditAction.ACCOUNT_LOCKED),
                        eq(user.getId()),
                        eq(user.getName()),
                        eq(user.getRole().name()),
                        any());
        then(userRepository).should(times(1)).save(user);
    }

    @Test
    @DisplayName("recordFailedAttempt() is a no-op for an unknown email")
    void recordFailedAttempt_unknownEmail_doesNothing() {
        given(userRepository.findByEmail("ghost@nowhere.za")).willReturn(Optional.empty());

        service.recordFailedAttempt("ghost@nowhere.za");

        then(userRepository).should(never()).save(any());
        then(auditPublisher).should(never()).publishAuthAudit(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("resetFailedAttempts() clears a positive attempt counter and saves")
    void resetFailedAttempts_positiveCount_resetsAndSaves() {
        user.setLoginAttempts(3);

        service.resetFailedAttempts(user);

        assertThat(user.getLoginAttempts()).isZero();
        then(userRepository).should(times(1)).save(user);
    }

    @Test
    @DisplayName("resetFailedAttempts() skips the save when attempts are already zero")
    void resetFailedAttempts_alreadyZero_skipsSave() {
        user.setLoginAttempts(0);

        service.resetFailedAttempts(user);

        then(userRepository).should(never()).save(any());
    }
}
