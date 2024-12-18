package com.loggingsystem.springjwtauth.exception;

import com.loggingsystem.springjwtauth.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    @ResponseBody
    public ErrorResponse handleCustomException(CustomException exception, HttpServletRequest request) {
        return new ErrorResponse(
                exception.getCode(),
                exception.getMessage(),
                exception.getStatus(),
                request.getRequestURI());
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
