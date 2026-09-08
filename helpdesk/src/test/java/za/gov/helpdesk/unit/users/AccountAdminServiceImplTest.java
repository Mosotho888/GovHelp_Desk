package za.gov.helpdesk.unit.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.gov.helpdesk.agent.service.AgentRoleLifecycleService;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auth.service.RefreshTokenService;
import za.gov.helpdesk.users.dto.response.UserResponse;
import za.gov.helpdesk.users.mapper.UserMapper;
import za.gov.helpdesk.users.model.Role;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;
import za.gov.helpdesk.users.service.UserQueryHelper;
import za.gov.helpdesk.users.service.impl.AccountAdminServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountAdminServiceImpl unit tests")
class AccountAdminServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserQueryHelper userQuery;
    @Mock private UserMapper userMapper;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AuditEventPublisher auditPublisher;
    @Mock private AgentRoleLifecycleService agentRoleLifecycle;

    private AccountAdminServiceImpl service;

    private User admin;
    private User target;

    @BeforeEach
    void setUp() {
        service =
                new AccountAdminServiceImpl(
                        userRepository,
                        userQuery,
                        userMapper,
                        refreshTokenService,
                        auditPublisher,
                        agentRoleLifecycle);

        admin = User.builder().id(1L).name("Admin").email("admin@gov.za").role(Role.ADMIN).build();
        target =
                User.builder()
                        .id(2L)
                        .name("John Public")
                        .email("john@citizen.za")
                        .role(Role.USER)
                        .active(true)
                        .build();
    }

    // ---- deactivateUser ----

    @Test
    @DisplayName("deactivateUser() deactivates the user, revokes tokens, and audits the change")
    void deactivateUser_activeOtherUser_deactivatesRevokesAndAudits() {
        given(userQuery.findOrThrow(2L)).willReturn(target);

        service.deactivateUser(2L, admin);

        assertThat(target.getActive()).isFalse();
        then(userRepository).should(times(1)).save(target);
        then(refreshTokenService).should(times(1)).revokeAll(target);
        then(auditPublisher)
                .should(times(1))
                .publishAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("deactivateUser() refuses to let an admin deactivate their own account")
    void deactivateUser_selfTarget_throws() {
        given(userQuery.findOrThrow(1L)).willReturn(admin);

        assertThatThrownBy(() -> service.deactivateUser(1L, admin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("own account");
        then(userRepository).should(org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("deactivateUser() refuses to re-deactivate an already-inactive user")
    void deactivateUser_alreadyInactive_throws() {
        target.setActive(false);
        given(userQuery.findOrThrow(2L)).willReturn(target);

        assertThatThrownBy(() -> service.deactivateUser(2L, admin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already inactive");
    }

    // ---- reactivateUser ----

    @Test
    @DisplayName("reactivateUser() reactivates the user and resets their failed login attempts")
    void reactivateUser_inactiveUser_reactivatesAndResetsAttempts() {
        target.setActive(false);
        target.setLoginAttempts(5);
        given(userQuery.findOrThrow(2L)).willReturn(target);

        service.reactivateUser(2L, admin);

        assertThat(target.getActive()).isTrue();
        assertThat(target.getLoginAttempts()).isZero();
        then(userRepository).should(times(1)).save(target);
    }

    @Test
    @DisplayName("reactivateUser() refuses to reactivate an already-active user")
    void reactivateUser_alreadyActive_throws() {
        given(userQuery.findOrThrow(2L)).willReturn(target);

        assertThatThrownBy(() -> service.reactivateUser(2L, admin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already active");
    }

    // ---- changeUserRole ----

    @Test
    @DisplayName(
            "changeUserRole() updates the role, triggers lifecycle handling, and revokes sessions")
    void changeUserRole_validChange_updatesTriggersLifecycleAndRevokes() {
        given(userQuery.findOrThrow(2L)).willReturn(target);
        given(userMapper.toUserResponse(target)).willReturn(UserResponse.builder().build());

        service.changeUserRole(2L, Role.AGENT, admin);

        assertThat(target.getRole()).isEqualTo(Role.AGENT);
        then(agentRoleLifecycle)
                .should(times(1))
                .handleRoleChange(target, Role.USER, Role.AGENT, admin);
        then(refreshTokenService).should(times(1)).revokeAll(target);
    }

    @Test
    @DisplayName("changeUserRole() rejects setting the same role the user already has")
    void changeUserRole_sameRole_throws() {
        given(userQuery.findOrThrow(2L)).willReturn(target);

        assertThatThrownBy(() -> service.changeUserRole(2L, Role.USER, admin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has role");
    }

    @Test
    @DisplayName("changeUserRole() refuses to let an admin change their own role")
    void changeUserRole_selfTarget_throws() {
        given(userQuery.findOrThrow(1L)).willReturn(admin);

        assertThatThrownBy(() -> service.changeUserRole(1L, Role.USER, admin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("own role");
    }
}
