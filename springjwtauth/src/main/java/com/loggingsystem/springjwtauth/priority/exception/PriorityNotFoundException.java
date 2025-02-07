package com.loggingsystem.springjwtauth.priority.exception;

import com.loggingsystem.springjwtauth.exception.ErrorMessages;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PriorityNotFoundException extends RuntimeException{
    public PriorityNotFoundException() {
        super(ErrorMessages.PRIORITY_NOT_FOUND.getMessage());
    }
}
