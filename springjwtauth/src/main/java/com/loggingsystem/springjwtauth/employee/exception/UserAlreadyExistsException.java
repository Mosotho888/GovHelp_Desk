package com.loggingsystem.springjwtauth.employee.exception;

import com.loggingsystem.springjwtauth.exception.ErrorMessages;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UserAlreadyExistsException extends RuntimeException{
    public UserAlreadyExistsException() {
        super(ErrorMessages.USER_ALREADY_EXISTS.getMessage());
    }
}
