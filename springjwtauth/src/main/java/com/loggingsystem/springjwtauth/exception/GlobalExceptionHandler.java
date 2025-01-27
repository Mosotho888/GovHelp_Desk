package com.loggingsystem.springjwtauth.exception;

import com.loggingsystem.springjwtauth.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.api.ErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException exception, HttpServletRequest request) {
        ErrorResponse errorMessages = new ErrorResponse(
                exception.getCode(),
                exception.getMessage(),
                exception.getStatus(),
                request.getRequestURI());

        return ResponseEntity.status(exception.getStatus()).body(errorMessages);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException exception, WebRequest request){
        Map<String, String> validationErrors = new HashMap<>();
        exception.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("errCode", "ERR_400_VALIDATION");
        response.put("message", "Validation failed for one or more fields");
        response.put("httpStatusCode", HttpStatus.BAD_REQUEST.value());
        response.put("httpStatus", HttpStatus.BAD_REQUEST.getReasonPhrase());
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("errors", validationErrors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

//    @ExceptionHandler(UserAlreadyExistsException.class)
//    @ResponseStatus(HttpStatus.CONFLICT)
//    @ResponseBody
//    public ErrorResponse handleUserAlreadyExistsException(UserAlreadyExistsException exception, HttpServletRequest request) {
//        return new ErrorResponse(exception.getMessage());
//    }
//
//    @ExceptionHandler(UsernameNotFoundException.class)
//    @ResponseStatus(HttpStatus.NOT_FOUND)
//    @ResponseBody
//    public ErrorResponse handleUsernameNotFoundException(UsernameNotFoundException exception, HttpServletRequest request) {
//        return new ErrorResponse(exception.getMessage());
//    }
//
//    @ExceptionHandler(TicketNotFoundException.class)
//    @ResponseStatus(HttpStatus.NOT_FOUND)
//    @ResponseBody
//    public ErrorResponse handleTicketNotFoundException(TicketNotFoundException exception, HttpServletRequest request) {
//        return new ErrorResponse(exception.getMessage());
//    }
//
//    @ExceptionHandler(TechnicianNotAuthorizedToUpdateTicketException.class)
//    @ResponseStatus(HttpStatus.FORBIDDEN)
//    @ResponseBody
//    public ErrorResponse handleTechnicianNotAuthorizedToUpdateTicketException(TechnicianNotAuthorizedToUpdateTicketException exception, HttpServletRequest request) {
//        return new ErrorResponse(exception.getMessage());
//    }

//    @ExceptionHandler({BadCredentialsException.class})
//    public ResponseEntity<Object> handleBadCredentialsException(BadCredentialsException exception) {
//        return ResponseEntity
//                .status(HttpStatus.UNAUTHORIZED)
//                .body(exception.getMessage());
//    }
}
