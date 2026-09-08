package za.gov.helpdesk.unit.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.exception.DuplicateResourceException;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.users.dto.request.ChangePasswordRequest;
import za.gov.helpdesk.users.dto.request.CreateUserRequest;
import za.gov.helpdesk.users.dto.request.UpdateUserRequest;
import za.gov.helpdesk.users.dto.response.UserResponse;
import za.gov.helpdesk.users.mapper.UserMapper;
import za.gov.helpdesk.users.model.Role;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;
import za.gov.helpdesk.users.service.PasswordManagementService;
import za.gov.helpdesk.users.service.UserQueryHelper;
import za.gov.helpdesk.users.service.impl.UserServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl unit tests")
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserQueryHelper userQuery;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditEventPublisher auditPublisher;
    @Mock private PasswordManagementService passwordManagementService;
    @Captor private ArgumentCaptor<User> userCaptor;

    private UserServiceImpl service;
    private User actor;

    @BeforeEach
    void setUp() {
        service =
                new UserServiceImpl(
                        userRepository,
                        userQuery,
                        userMapper,
                        passwordEncoder,
                        auditPublisher,
                        passwordManagementService);
        actor = User.builder().id(1L).name("Admin").email("admin@gov.za").role(Role.ADMIN).build();
    }

    // ---- createUser ----

    @Test
    @DisplayName(
            "createUser() normalises the email, encodes the password, and defaults role/timezone")
    void createUser_minimalRequest_appliesDefaults() {
        final CreateUserRequest request = new CreateUserRequest();
        request.setName("John Public");
        request.setEmail("  John@Citizen.ZA  ");
        request.setPassword("plainPass1");

        given(userRepository.existsByEmail("  John@Citizen.ZA  ")).willReturn(false);
        given(passwordEncoder.encode("plainPass1")).willReturn("hashed");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));
        given(userMapper.toUserResponse(any(User.class)))
                .willReturn(UserResponse.builder().build());

        service.createUser(request, actor);

        then(userRepository).should(times(1)).save(userCaptor.capture());
        final User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("john@citizen.za");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getTimezone()).isEqualTo("Africa/Johannesburg");
        assertThat(saved.getActive()).isTrue();
    }

    @Test
    @DisplayName("createUser() honours an explicitly requested role and timezone")
    void createUser_explicitRoleAndTimezone_respectsRequest() {
        final CreateUserRequest request = new CreateUserRequest();
        request.setName("Jane Agent");
        request.setEmail("jane@gov.za");
        request.setPassword("plainPass1");
        request.setRole(Role.AGENT);
        request.setTimezone("UTC");

        given(userRepository.existsByEmail("jane@gov.za")).willReturn(false);
        given(passwordEncoder.encode("plainPass1")).willReturn("hashed");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));
        given(userMapper.toUserResponse(any(User.class)))
                .willReturn(UserResponse.builder().build());

        service.createUser(request, actor);

        then(userRepository).should(times(1)).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.AGENT);
        assertThat(userCaptor.getValue().getTimezone()).isEqualTo("UTC");
    }

    @Test
    @DisplayName("createUser() rejects an email that is already registered")
    void createUser_duplicateEmail_throws() {
        final CreateUserRequest request = new CreateUserRequest();
        request.setEmail("john@citizen.za");
        given(userRepository.existsByEmail("john@citizen.za")).willReturn(true);

        assertThatThrownBy(() -> service.createUser(request, actor))
                .isInstanceOf(DuplicateResourceException.class);
        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName(
            "createUser() converts a race-condition constraint violation into a friendly duplicate"
                    + " error")
    void createUser_concurrentDuplicateInsert_translatesException() {
        final CreateUserRequest request = new CreateUserRequest();
        request.setName("John Public");
        request.setEmail("john@citizen.za");
        request.setPassword("plainPass1");

        given(userRepository.existsByEmail("john@citizen.za")).willReturn(false);
        given(passwordEncoder.encode("plainPass1")).willReturn("hashed");
        given(userRepository.save(any(User.class)))
                .willThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> service.createUser(request, actor))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // ---- getUserById / getUserByEmail ----

    @Test
    @DisplayName("getUserById() delegates lookup and mapping")
    void fetchUserById_existingId_returnsMappedResponse() {
        final User user = User.builder().id(2L).name("John Public").build();
        given(userQuery.findOrThrow(2L)).willReturn(user);
        final UserResponse response = UserResponse.builder().id(2L).build();
        given(userMapper.toUserResponse(user)).willReturn(response);

        assertThat(service.getUserById(2L)).isEqualTo(response);
    }

    @Test
    @DisplayName("getUserById() propagates ResourceNotFoundException for an unknown id")
    void fetchUserById_unknownId_throws() {
        given(userQuery.findOrThrow(999L)).willThrow(new ResourceNotFoundException("User", 999L));

        assertThatThrownBy(() -> service.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getUserByEmail() delegates lookup and mapping")
    void fetchUserByEmail_existingEmail_returnsMappedResponse() {
        final User user = User.builder().id(2L).email("john@citizen.za").build();
        given(userQuery.findByEmailOrThrow("john@citizen.za")).willReturn(user);
        final UserResponse response = UserResponse.builder().id(2L).build();
        given(userMapper.toUserResponse(user)).willReturn(response);

        assertThat(service.getUserByEmail("john@citizen.za")).isEqualTo(response);
    }

    // ---- updateUser ----

    @Test
    @DisplayName("updateUser() applies a name change and publishes an audit entry describing it")
    void updateUser_nameChanged_updatesAndAudits() {
        final User user = User.builder().id(2L).name("Old Name").build();
        given(userQuery.findOrThrow(2L)).willReturn(user);
        given(userRepository.save(user)).willReturn(user);
        given(userMapper.toUserResponse(user)).willReturn(UserResponse.builder().build());

        final UpdateUserRequest request = new UpdateUserRequest();
        request.setName("New Name");

        service.updateUser(2L, request, actor);

        assertThat(user.getName()).isEqualTo("New Name");
        then(auditPublisher)
                .should(times(1))
                .publishAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("updateUser() skips auditing when no fields actually changed")
    void updateUser_noActualChanges_skipsAudit() {
        final User user = User.builder().id(2L).name("Same Name").build();
        given(userQuery.findOrThrow(2L)).willReturn(user);
        given(userRepository.save(user)).willReturn(user);
        given(userMapper.toUserResponse(user)).willReturn(UserResponse.builder().build());

        final UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Same Name");

        service.updateUser(2L, request, actor);

        then(auditPublisher)
                .should(never())
                .publishAudit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName(
            "updateUser() saves the entity via the repository regardless of whether fields changed")
    void updateUser_anyRequest_alwaysSaves() {
        final User user = User.builder().id(2L).name("Old Name").build();
        given(userQuery.findOrThrow(2L)).willReturn(user);
        given(userRepository.save(user)).willReturn(user);
        given(userMapper.toUserResponse(user)).willReturn(UserResponse.builder().build());

        service.updateUser(2L, new UpdateUserRequest(), actor);

        then(userRepository).should(times(1)).save(user);
    }

    // ---- saveUser / changeOwnPassword delegation ----

    @Test
    @DisplayName("saveUser() delegates directly to the repository")
    void saveUser_anyUser_delegatesToRepository() {
        final User user = User.builder().id(2L).build();
        given(userRepository.save(user)).willReturn(user);

        assertThat(service.saveUser(user)).isEqualTo(user);
    }

    @Test
    @DisplayName("changeOwnPassword() delegates to the PasswordManagementService")
    void changeOwnPassword_anyRequest_delegatesToPasswordManagementService() {
        final ChangePasswordRequest request = new ChangePasswordRequest();

        service.changeOwnPassword(request, actor);

        then(passwordManagementService).should(times(1)).changeOwnPassword(request, actor);
    }
}
