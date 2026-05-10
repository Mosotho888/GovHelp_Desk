package za.gov.helpdesk.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import za.gov.helpdesk.auth.dto.AuthResponse;
import za.gov.helpdesk.auth.dto.LoginRequest;
import za.gov.helpdesk.auth.jwt.JwtUtil;
import za.gov.helpdesk.auth.service.AuthService;
import za.gov.helpdesk.config.security.JwtProperties;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService unit tests")
public class AuthServiceImpITest {

    @Mock
    private AuthenticationManager authManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test Agen")
                .email("agent@gov.za")
                .passwordHash("$2a$12$xUDVJeAkGqB7hUk0je5oZe2s569JMXdQXU.bIyViyZrF2SBxWtuei")  // Password@123
                .role(User.Role.AGENT)
                .active(true)
                .loginAttempts(0)
                .timezone("Afria/Johannesburg")
                .build();

    }

    @Test
    @DisplayName("login() returns token on valid credentials")
    void login_validCredentials_returnTokens() {
        // Given
        String email = "agent@gov.za";
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("Password@123");

        given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
        given(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities()));
        given(jwtUtil.generateAccessToken(testUser)).willReturn("access.token.here");
        given(jwtUtil.generateRefreshToken(testUser)).willReturn("refresh.token.here");
        given(userRepository.save(any(User.class))).willReturn(testUser);

        // When
        AuthResponse authResponse = authService.login(request);

        // Then
        assertThat(authResponse.getAccessToken()).isEqualTo("access.token.here");
        assertThat(authResponse.getRefreshToken()).isEqualTo("refresh.token.here");
        assertThat(authResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(authResponse.getUser().getEmail()).isEqualTo(email);
        then(userRepository).should().save(argThat(user -> user.getLoginAttempts() == 0));
    }

    @Test
    @DisplayName("login() increments counter on bad credentials")
    void login_badCredentials_incrementsAttempts() {
        // Given
        String email = "agent@gov.za";
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("WrongPassword@123");

        given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
        given(authManager.authenticate(any())).willThrow(new BadCredentialsException("Bad credentials"));
        given(userRepository.save(any(User.class))).willReturn(testUser);

        // When / Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
        then(userRepository).should().save(argThat(user -> user.getLoginAttempts() == 1));
    }

    @Test
    @DisplayName("login() deactivate account after failed attempts")
    void login_maxAttempts_deactivateAccount() {
        // Given
        String email = "agent@gov.za";
        testUser.setLoginAttempts(4);
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("WrongPassword@123");

        given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
        given(authManager.authenticate(any())).willThrow(new BadCredentialsException("Bad credentials"));
        given(userRepository.save(any(User.class))).willReturn(testUser);

        // When / Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
        then(userRepository).should().save(argThat(user -> user.getActive() == false));

    }
}
