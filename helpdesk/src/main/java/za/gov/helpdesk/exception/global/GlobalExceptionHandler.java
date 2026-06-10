package za.gov.helpdesk.exception.global;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import za.gov.helpdesk.exception.DuplicateResourceException;
import za.gov.helpdesk.exception.InvalidTokenException;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.exception.dto.response.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import za.gov.helpdesk.ticket.exception.InvalidStatusTransitionException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> ApiErrorResponse.FieldError.builder()
                        .field(e.getField())
                        .issue(e.getDefaultMessage())
                        .build())
                .toList();

        return ResponseEntity.badRequest().body(
                ApiErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(400)
                        .error("VALIDATION_ERROR")
                        .message("Request validation failed")
                        .path(req.getRequestURI())
                        .details(fieldErrors)
                        .build()
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, "NOT_FOUND", ex.getMessage(), req));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(
            DuplicateResourceException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error(409, "CONFLICT", ex.getMessage(), req));
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidTransition(
            InvalidStatusTransitionException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(error(422, "INVALID_STATUS_TRANSITION", ex.getMessage(), req));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest req) {

        log.warn("Failed login attempt on path: {}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(error(401, "INVALID_CREDENTIALS", "Invalid email or password", req));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidToken(
            InvalidTokenException ex, HttpServletRequest req) {

        log.warn("Invalid token on path: {}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(error(401, "INVALID_TOKEN", "Malformed token", req)); // 💡 Uses the actual dynamic message
    }

    @ExceptionHandler({LockedException.class, DisabledException.class})
    public ResponseEntity<ApiErrorResponse> handleLocked(
            RuntimeException ex, HttpServletRequest req) {

        log.warn("Blocked access attempt to locked/disabled account: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(403, "ACCOUNT_LOCKED", "Account locked. Contact your administrator.", req));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(403, "FORBIDDEN", "You do not have permission to perform this action", req));
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest req) {

        log.error("Unhandled exception caught on path: {}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(500, "INTERNAL_ERROR", "An unexpected error occurred", req));
    }

    private ApiErrorResponse error(int status, String code, String message, HttpServletRequest req) {
        return ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(code)
                .message(message)
                .path(req.getRequestURI())
                .build();
    }
}
