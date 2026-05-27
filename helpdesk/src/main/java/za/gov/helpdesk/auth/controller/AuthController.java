package za.gov.helpdesk.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import za.gov.helpdesk.auth.dto.request.PasswordResetConfirmRequest;
import za.gov.helpdesk.auth.dto.request.PasswordResetRequest;
import za.gov.helpdesk.auth.dto.response.AuthResponse;
import za.gov.helpdesk.auth.dto.request.RefreshTokenRequest;
import za.gov.helpdesk.auth.service.AuthService;
import za.gov.helpdesk.auth.dto.request.LoginRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.gov.helpdesk.auth.service.PasswordResetService;
import za.gov.helpdesk.users.security.CustomUserDetails;

import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and token management")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive JWT tokens")
    public ResponseEntity<AuthResponse> login (@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke refresh token")
    public ResponseEntity<Void> logout(
            @RequestBody RefreshTokenRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        authService.logout(request.getRefreshToken(), principal.getUser());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    @Operation(
            summary = "Request a password reset OTP",
            description = "Sends a 6-digit OTP to the email if it exists. Always returns 200 to prevent user enumeration."
    )
    public ResponseEntity<Map<String, String>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {

        passwordResetService.requestReset(request);

        // Always same response — don't reveal if email exists
        return ResponseEntity.ok(Map.of(
                "message", "If that email is registered you will receive a reset code shortly"
        ));
    }

    @PostMapping("/password-reset/confirm")
    @Operation(
            summary = "Confirm OTP and set new password",
            description = "Validates the OTP and updates the password. All active sessions are revoked."
    )
    public ResponseEntity<Map<String, String>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request) {

        passwordResetService.confirmReset(request);

        return ResponseEntity.ok(Map.of(
                "message", "Password updated successfully. Please log in with your new password."
        ));
    }
}
