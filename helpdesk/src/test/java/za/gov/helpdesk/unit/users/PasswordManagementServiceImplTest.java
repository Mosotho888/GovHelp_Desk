package za.gov.helpdesk.unit.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auth.service.RefreshTokenService;
import za.gov.helpdesk.users.dto.request.AdminPasswordResetRequest;
import za.gov.helpdesk.users.dto.request.ChangePasswordRequest;
import za.gov.helpdesk.users.model.Role;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;
import za.gov.helpdesk.users.service.UserQueryHelper;
import za.gov.helpdesk.users.service.impl.PasswordManagementServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordManagementServiceImpl unit tests")
class PasswordManagementServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserQueryHelper userQuery;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AuditEventPublisher auditPublisher;

    private PasswordManagementServiceImpl service;

    private User user;
    private User admin;

    @BeforeEach
    void setUp() {
        service =
                new PasswordManagementServiceImpl(
                        userRepository,
                        userQuery,
                        passwordEncoder,
                        refreshTokenService,
                        auditPublisher);

        user =
                User.builder()
                        .id(2L)
                        .name("John Public")
                        .email("john@citizen.za")
                        .passwordHash("old-hash")
                        .active(true)
                        .build();
        admin = User.builder().id(1L).name("Admin").email("admin@gov.za").role(Role.ADMIN).build();
    }

    // ---- changeOwnPassword ----

    @Test
    @DisplayName("changeOwnPassword() updates the hash, revokes sessions, and audits the change")
    void changeOwnPassword_validRequest_updatesRevokesAndAudits() {
        final ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPass1");
        request.setNewPassword("newPass1");

        given(userQuery.findOrThrow(2L)).willReturn(user);
        given(passwordEncoder.matches("oldPass1", "old-hash")).willReturn(true);
        given(passwordEncoder.matches("newPass1", "old-hash")).willReturn(false);
        given(passwordEncoder.encode("newPass1")).willReturn("new-hash");

        service.changeOwnPassword(request, user);

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        then(userRepository).should(times(1)).save(user);
        then(refreshTokenService).should(times(1)).revokeAll(user);
        then(auditPublisher)
                .should(times(1))
                .publishAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("changeOwnPassword() rejects an inactive account")
    void changeOwnPassword_inactiveAccount_throws() {
        user.setActive(false);
        given(userQuery.findOrThrow(2L)).willReturn(user);

        final ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPass1");
        request.setNewPassword("newPass1");

        assertThatThrownBy(() -> service.changeOwnPassword(request, user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inactive account");
        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("changeOwnPassword() rejects an incorrect current password")
    void changeOwnPassword_wrongCurrentPassword_throwsBadCredentials() {
        given(userQuery.findOrThrow(2L)).willReturn(user);
        given(passwordEncoder.matches("wrongPass", "old-hash")).willReturn(false);

        final ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPass");
        request.setNewPassword("newPass1");

        assertThatThrownBy(() -> service.changeOwnPassword(request, user))
                .isInstanceOf(BadCredentialsException.class);
        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("changeOwnPassword() rejects a new password identical to the current one")
    void changeOwnPassword_sameAsCurrentPassword_throws() {
        given(userQuery.findOrThrow(2L)).willReturn(user);
        given(passwordEncoder.matches("oldPass1", "old-hash")).willReturn(true);

        final ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPass1");
        request.setNewPassword("oldPass1");

        assertThatThrownBy(() -> service.changeOwnPassword(request, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must differ");
        then(userRepository).should(never()).save(any());
    }

    // ---- adminResetPassword ----

    @Test
    @DisplayName(
            "adminResetPassword() resets the target's password and clears their lockout counter")
    void adminResetPassword_validRequest_resetsPasswordAndUnlocks() {
        user.setLoginAttempts(5);
        given(userQuery.findOrThrow(2L)).willReturn(user);
        given(passwordEncoder.encode("brandNewPass1")).willReturn("reset-hash");

        final AdminPasswordResetRequest request = new AdminPasswordResetRequest();
        request.setNewPassword("brandNewPass1");
        request.setReason("User locked out and lost access to email");

        service.adminResetPassword(2L, request, admin);

        assertThat(user.getPasswordHash()).isEqualTo("reset-hash");
        assertThat(user.getLoginAttempts()).isZero();
        then(userRepository).should(times(1)).save(user);
        then(refreshTokenService).should(times(1)).revokeAll(user);
    }

    @Test
    @DisplayName("adminResetPassword() refuses to let an admin reset their own password this way")
    void adminResetPassword_selfTarget_throws() {
        final AdminPasswordResetRequest request = new AdminPasswordResetRequest();
        request.setNewPassword("brandNewPass1");

        assertThatThrownBy(() -> service.adminResetPassword(1L, request, admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("regular password change flow");
        then(userRepository).should(never()).save(any());
    }
}
