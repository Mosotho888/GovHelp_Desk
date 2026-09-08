package za.gov.helpdesk.unit.services.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import za.gov.helpdesk.auth.dto.response.AuthResponse;
import za.gov.helpdesk.auth.jwt.JwtService;
import za.gov.helpdesk.auth.service.AuthResponseFactory;
import za.gov.helpdesk.users.dto.response.UserResponse;
import za.gov.helpdesk.users.mapper.UserMapper;
import za.gov.helpdesk.users.model.Role;
import za.gov.helpdesk.users.model.User;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthResponseFactory unit tests")
class AuthResponseFactoryTest {

    @Mock private JwtService jwtService;
    @Mock private UserMapper userMapper;

    private AuthResponseFactory factory;
    private User user;

    @BeforeEach
    void setUp() {
        factory = new AuthResponseFactory(jwtService, userMapper);
        ReflectionTestUtils.setField(factory, "accessTokenExpiryMs", 3_600_000L);

        user =
                User.builder()
                        .id(1L)
                        .name("Jane Agent")
                        .email("jane@gov.za")
                        .role(Role.AGENT)
                        .active(true)
                        .build();
    }

    @Test
    @DisplayName(
            "build() assembles a response with a fresh access token, the given refresh token, and"
                    + " mapped user")
    void build_validUser_assemblesCompleteResponse() {
        given(jwtService.generateAccessToken(user)).willReturn("new-access-token");
        final UserResponse mappedUser = UserResponse.builder().id(1L).email("jane@gov.za").build();
        given(userMapper.toUserResponse(user)).willReturn(mappedUser);

        final AuthResponse response = factory.build(user, "existing-refresh-token");

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("existing-refresh-token");
        assertThat(response.getExpiresIn()).isEqualTo(3_600L);
        assertThat(response.getUser()).isEqualTo(mappedUser);
    }

    @Test
    @DisplayName("build() converts the millisecond expiry configuration into whole seconds")
    void build_anyUser_convertsExpiryToSeconds() {
        ReflectionTestUtils.setField(factory, "accessTokenExpiryMs", 900_000L);
        given(jwtService.generateAccessToken(user)).willReturn("token");
        given(userMapper.toUserResponse(user)).willReturn(UserResponse.builder().build());

        final AuthResponse response = factory.build(user, "refresh");

        assertThat(response.getExpiresIn()).isEqualTo(900L);
    }
}
