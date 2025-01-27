package com.loggingsystem.springjwtauth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//@ResponseStatus(HttpStatus.CONFLICT)
public class UserAlreadyExistsException extends CustomException{
    public UserAlreadyExistsException() {
        
        super(ErrorMessages.USER_ALREADY_EXISTS);
    }
}
