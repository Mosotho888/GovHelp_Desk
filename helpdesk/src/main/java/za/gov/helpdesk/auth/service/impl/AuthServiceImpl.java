package za.gov.helpdesk.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.auth.dto.AuthResponse;
import za.gov.helpdesk.auth.dto.RefreshTokenRequest;
import za.gov.helpdesk.auth.jwt.JwtUtil;
import za.gov.helpdesk.auth.dto.LoginRequest;
import za.gov.helpdesk.auth.service.AuthService;
import za.gov.helpdesk.config.security.JwtProperties;
import za.gov.helpdesk.users.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final String TOKEN_REFRESH = "refresh";
    private final AuthenticationManager authenticationManager;
    private final za.gov.helpdesk.users.repository.UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        za.gov.helpdesk.users.model.User employee = userRepository.findByEmail(loginRequest.userEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.userEmail(), loginRequest.password())
            );
        } catch (AuthenticationException ex) {
            // Increment failed attempt counter
            employee.setLoginAttempts(employee.getLoginAttempts() + 1);
            if (employee.getLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
                employee.setActive(false);
            }
            userRepository.save(employee);
            throw ex;
        }

        // Reset on successful login
        employee.setLoginAttempts(0);
        userRepository.save(employee);

        return buildAuthResponse(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest refreshToken) {
        String email = jwtUtil.extractEmail(refreshToken.getRefreshToken());

        za.gov.helpdesk.users.model.User employee = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        if (!TOKEN_REFRESH.equals(jwtUtil.extractTokenType(refreshToken.getRefreshToken()))
                || jwtUtil.isTokenExpired(refreshToken.getRefreshToken())) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        return buildAuthResponse(employee);
    }

    private AuthResponse buildAuthResponse(za.gov.helpdesk.users.model.User employee) {
        return AuthResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(employee))
                .refreshToken(jwtUtil.generateRefreshToken(employee))
                .expiresIn(jwtProperties.getValidity() / 1000)
                .user(toEmployeeResponse(employee))
                .build();
    }

    private za.gov.helpdesk.users.dto.UserResponse toEmployeeResponse(za.gov.helpdesk.users.model.User employee) {
        return za.gov.helpdesk.users.dto.UserResponse.builder()
                .id(employee.getId())
                .name(employee.getName())
                .email(employee.getEmail())
                .role(employee.getRole())
                .phone(employee.getPhone())
                .timezone(employee.ge)
                .active(employee.getActive())
                .createdAt(employee.getCreatedAt())
                .build();
    }
}
