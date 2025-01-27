package com.loggingsystem.springjwtauth.exception;

import com.loggingsystem.springjwtauth.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleUserAlreadyExistsException(UserAlreadyExistsException exception, WebRequest request) {
        return createErrorResponse(exception, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleUserNotFoundException(UsernameNotFoundException exception, WebRequest request) {
        return createErrorResponse(exception, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(TechnicianNotAuthorizedToUpdateTicketException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleTechnicianNotAuthorizedToUpdateTicketException(TechnicianNotAuthorizedToUpdateTicketException exception, WebRequest request) {
        return createErrorResponse(exception, HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(TicketNotFoundException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleTicketNotFoundException(TicketNotFoundException exception, WebRequest request) {
        return createErrorResponse(exception, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(PriorityNotFoundException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handlePriorityNotFoundException(PriorityNotFoundException exception, WebRequest request) {
        return createErrorResponse(exception, HttpStatus.NOT_FOUND, request);
    }

    private ErrorResponse createErrorResponse(Exception exception, HttpStatus status, WebRequest request) {
        return new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
    }
}
