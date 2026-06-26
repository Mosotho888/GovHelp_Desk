package za.gov.helpdesk.auth.controller;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import za.gov.helpdesk.auth.dto.request.LoginRequest;
import za.gov.helpdesk.auth.dto.request.PasswordResetConfirmRequest;
import za.gov.helpdesk.auth.dto.request.PasswordResetRequest;
import za.gov.helpdesk.auth.dto.request.RefreshTokenRequest;
import za.gov.helpdesk.auth.dto.response.AuthResponse;
import za.gov.helpdesk.auth.service.AuthService;
import za.gov.helpdesk.auth.service.PasswordResetService;
import za.gov.helpdesk.users.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * REST controller managing authentication operations, credential evaluation, session termination,
 * and secure self-service password reset workflows.
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and token management")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    /**
     * Authenticates primary user account credentials (identity handle and secret key). Upon
     * success, generates a cryptographically signed short-lived access JWT and a durable refresh
     * token.
     *
     * @param loginRequest the structured credential payload transmitted by the client
     * @return a {@link ResponseEntity} wrapping the generated {@link AuthResponse} tokens
     */
    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive JWT tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody final LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    /**
     * Exchanges a valid, unrevoked tracking refresh token to obtain a fresh short-lived access
     * token. Allows clients to extend their active user sessions without re-entering credentials.
     *
     * @param request the payload containing the active refresh token string
     * @return a {@link ResponseEntity} wrapping the newly issued token pairs
     */
    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new token")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody final RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /**
     * Reclaims and terminates an active user session by explicitly revoking the supplied refresh
     * token. Prevents further access key generation from that specific session context.
     *
     * @param request the payload containing the refresh token targeted for destruction
     * @param principal the authenticated security context of the user executing the sign-out
     *     operation
     * @return a blank {@link ResponseEntity} confirming status 204 No Content
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke refresh token")
    public ResponseEntity<Void> logout(
            @RequestBody final RefreshTokenRequest request,
            @AuthenticationPrincipal final CustomUserDetails principal) {

        authService.logout(request.getRefreshToken(), principal.getUser());
        return ResponseEntity.noContent().build();
    }

    /**
     * Initiates a password reset process by generating a single-use 6-digit verification code. To
     * prevent user enumeration attacks, this endpoint returns a generic success message regardless
     * of whether the requested email exists within the identity repository.
     *
     * @param request the payload containing the destination account email string
     * @return a {@link ResponseEntity} containing a generic confirmation notification message map
     */
    @PostMapping("/password-reset/request")
    @Operation(
            summary = "Request a password reset OTP",
            description =
                    "Sends a 6-digit OTP to the email if it exists. Always returns 200 to prevent "
                            + "user enumeration.")
    public ResponseEntity<Map<String, String>> requestPasswordReset(
            @Valid @RequestBody final PasswordResetRequest request) {

        passwordResetService.requestReset(request);

        // Always same response — don't reveal if email exists
        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "If that email is registered you will receive a reset code shortly"));
    }

    /**
     * Validates an account ownership verification code and overrides the historical password hash.
     * Forces the invalidation and revocation of all currently active refresh tokens linked to the
     * account.
     *
     * @param request the payload mapping the verification code alongside the new target password
     *     configuration
     * @return a {@link ResponseEntity} containing a success notification message map
     */
    @PostMapping("/password-reset/confirm")
    @Operation(
            summary = "Confirm OTP and set new password",
            description =
                    "Validates the OTP and updates the password. All active sessions are revoked.")
    public ResponseEntity<Map<String, String>> confirmPasswordReset(
            @Valid @RequestBody final PasswordResetConfirmRequest request) {

        passwordResetService.confirmReset(request);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Password updated successfully. Please log in with your new password."));
    }
}
