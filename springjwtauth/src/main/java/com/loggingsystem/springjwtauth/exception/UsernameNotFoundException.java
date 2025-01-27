package com.loggingsystem.springjwtauth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UsernameNotFoundException extends RuntimeException{
    public UsernameNotFoundException() {
        super(ErrorMessages.USERNAME_NOT_FOUND.getMessage());
    }
}
