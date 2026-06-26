package za.gov.helpdesk.exception.global;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import za.gov.helpdesk.exception.DuplicateResourceException;
import za.gov.helpdesk.exception.InvalidTokenException;
import za.gov.helpdesk.exception.RateLimitExceededException;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.exception.StorageException;
import za.gov.helpdesk.exception.dto.response.ApiErrorResponse;
import za.gov.helpdesk.ticket.exception.InvalidStatusTransitionException;

import lombok.extern.slf4j.Slf4j;

/**
 * Centralized global exception translation interceptor layer for the REST API perimeter. Intercepts
 * thrown business errors, security runtime blockages, and input validation bounds failures across
 * all controllers, transforming them into uniform, structured {@link ApiErrorResponse} JSON
 * payloads.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Intercepts and handles data binding validation exceptions thrown when request payload fields
     * fail structural constraints defined via JSR-380 Bean Validation annotations. Extracts and
     * maps field-level violations into granular error summaries.
     *
     * @param ex the active validation failure wrapper containing detailed field binding logs
     * @param req the incoming servlet request metadata wrapper containing target context details
     * @return a {@link ResponseEntity} wrapping the compiled validation-specific {@link
     *     ApiErrorResponse}
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            final MethodArgumentNotValidException ex, final HttpServletRequest req) {

        final List<ApiErrorResponse.FieldError> fieldErrors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(
                                e ->
                                        ApiErrorResponse.FieldError.builder()
                                                .field(e.getField())
                                                .issue(e.getDefaultMessage())
                                                .build())
                        .toList();

        return ResponseEntity.badRequest()
                .body(
                        ApiErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("VALIDATION_ERROR")
                                .message("Request validation failed")
                                .path(req.getRequestURI())
                                .details(fieldErrors)
                                .build());
    }

    /**
     * Translates core business resource absences into standardized HTTP 404 status payloads.
     *
     * @param ex the resource identity lookup failure context containing missing record details
     * @param req the active servlet request tracking parameters
     * @return a structured 404 status error representation
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            final ResourceNotFoundException ex, final HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", ex.getMessage(), req));
    }

    /**
     * Catches and translates database unique constraint violations into HTTP 409 status responses.
     *
     * @param ex the duplicate unique data initialization failure context
     * @param req the active servlet request tracking parameters
     * @return a structured 409 status error representation
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(
            final DuplicateResourceException ex, final HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(HttpStatus.CONFLICT.value(), "CONFLICT", ex.getMessage(), req));
    }

    /**
     * Captures and handles illegal workflow status mutations on ticket life cycles, translating
     * them into explicit HTTP 422 Unprocessable Entity outcomes.
     *
     * @param ex the state machine processing barrier exception tracking the invalid transition path
     * @param req the active servlet request tracking parameters
     * @return a structured 422 status error representation
     */
    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidTransition(
            final InvalidStatusTransitionException ex, final HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(
                        error(
                                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                                "INVALID_STATUS_TRANSITION",
                                ex.getMessage(),
                                req));
    }

    /**
     * Intercepts bad authentication attempts, logs a localized alert warning trace, and produces an
     * HTTP 411 Unauthorized validation contract back to the caller.
     *
     * @param ex the primary identity password match processing failure wrapper
     * @param req the active servlet request tracking parameters
     * @return a structured 401 status error representation with sanitized disclosure messages
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            final BadCredentialsException ex, final HttpServletRequest req) {

        log.warn(
                "Failed login attempt on path: {} [Reason: {}]",
                req.getRequestURI(),
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        error(
                                HttpStatus.UNAUTHORIZED.value(),
                                "INVALID_CREDENTIALS",
                                "Invalid email or password",
                                req));
    }

    /**
     * Fallback exception trap catching generalized authentication discrepancies passed out of the
     * security chain filters that were not intercepted by more explicit exception parameters.
     *
     * @param ex the core security authentication failure tracking instance
     * @param req the active servlet request tracking parameters
     * @return a structured 401 status error representation
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationFallback(
            final AuthenticationException ex, final HttpServletRequest req) {

        log.warn(
                "Authentication roadblock intercepted on path {}: {}",
                req.getRequestURI(),
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        error(
                                HttpStatus.UNAUTHORIZED.value(),
                                "UNAUTHORIZED",
                                "Authentication required: " + ex.getMessage(),
                                req));
    }

    /**
     * Intercepts rate limiting threshold infractions, converting them into uniform HTTP 429 status
     * payloads.
     *
     * @param ex the active rate limit infrastructure breach tracking context
     * @param req the active servlet request tracking parameters
     * @return a structured 429 status error representation
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimitExceeded(
            final RateLimitExceededException ex, final HttpServletRequest req) {

        log.warn(
                "Rate limit exceeded on path: {} [Message: {}]",
                req.getRequestURI(),
                ex.getMessage());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(
                        error(
                                HttpStatus.TOO_MANY_REQUESTS.value(),
                                "RATE_LIMIT_EXCEEDED",
                                ex.getMessage(),
                                req));
    }

    /**
     * Intercepts and handles application-wide {@link StorageException} occurrences. Logs the
     * localized underlying error message and builds a standardized API error payload with an HTTP
     * 500 Internal Server Error classification.
     *
     * @param ex the runtime storage exception containing the root failure details
     * @param req the incoming servlet request providing context for the error path mapping
     * @return a {@link ResponseEntity} wrapping the standardized {@link ApiErrorResponse} payload
     */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiErrorResponse> handleStorageException(
            final StorageException ex, final HttpServletRequest req) {

        log.error("Storage management failure intercepted: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        error(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "STORAGE ERROR",
                                "Check storage",
                                req));
    }

    /**
     * Intercepts cryptographically broken, unparsable, or signature-expired session tracking
     * tokens.
     *
     * @param ex the JWT parsing constraint boundary exception
     * @param req the active servlet request tracking parameters
     * @return a structured 401 status error representation
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidToken(
            final InvalidTokenException ex, final HttpServletRequest req) {

        log.warn("Invalid token on path: {}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        error(
                                HttpStatus.UNAUTHORIZED.value(),
                                "INVALID_TOKEN",
                                "Malformed token",
                                req));
    }

    /**
     * Traps requests aimed at identity profiles that have been explicitly disabled or frozen via
     * administrator lock parameters, routing back an HTTP 403 Forbidden payload.
     *
     * @param ex the account status lockdown configuration runtime constraint exception
     * @param req the active servlet request tracking parameters
     * @return a structured 403 status error representation
     */
    @ExceptionHandler({LockedException.class, DisabledException.class})
    public ResponseEntity<ApiErrorResponse> handleLocked(
            final RuntimeException ex, final HttpServletRequest req) {

        log.warn("Blocked access attempt to locked/disabled account: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        error(
                                HttpStatus.FORBIDDEN.value(),
                                "ACCOUNT_LOCKED",
                                "Account locked. Contact your administrator.",
                                req));
    }

    /**
     * Captures role or identity entitlement validation mismatches thrown by method-level Spring
     * Security protection hooks, creating uniform HTTP 403 authorization responses.
     *
     * @param ex the core security authorization rule clearing failure
     * @param req the active servlet request tracking parameters
     * @return a structured 403 status error representation
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            final AccessDeniedException ex, final HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        error(
                                HttpStatus.FORBIDDEN.value(),
                                "FORBIDDEN",
                                "You do not have permission to perform this action",
                                req));
    }

    /**
     * Serves as the global fallback safety net handler catching any unhandled runtime exceptions
     * that cascade up out of service transactions. Emits a prioritized log error trace for system
     * monitoring.
     *
     * @param ex the root unmapped server execution failure trace
     * @param req the active servlet request tracking parameters
     * @return an obfuscated HTTP 500 status error container hiding low-level internal technology
     *     leaks
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            final Exception ex, final HttpServletRequest req) {

        log.error("Unhandled exception caught on path: {}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        error(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "INTERNAL_ERROR",
                                "An unexpected error occurred",
                                req));
    }

    /**
     * Factory utility encapsulating instantiation and compilation of flat, basic error response
     * structures.
     *
     * @param status the HTTP integer state marker code to declare
     * @param code a machine-readable internal unified string exception categorization tracking
     *     constant
     * @param message a descriptive readable summary clarifying data conditions
     * @param req the incoming request context trace mapping path parameters
     * @return a populated ready-to-serialize {@link ApiErrorResponse} instance
     */
    private ApiErrorResponse error(
            final int status,
            final String code,
            final String message,
            final HttpServletRequest req) {
        return ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(code)
                .message(message)
                .path(req.getRequestURI())
                .build();
    }
}
