package com.loggingsystem.springjwtauth.employee.exception;

import com.loggingsystem.springjwtauth.exception.ErrorMessages;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException{
    public UserNotFoundException() {
        super(ErrorMessages.USERNAME_NOT_FOUND.getMessage());
    }
}
