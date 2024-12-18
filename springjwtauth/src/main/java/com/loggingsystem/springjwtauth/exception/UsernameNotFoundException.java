package com.loggingsystem.springjwtauth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UsernameNotFoundException extends CustomException{
    public UsernameNotFoundException() {
        super(ErrorMessages.USERNAME_NOT_FOUND);
    }
}
