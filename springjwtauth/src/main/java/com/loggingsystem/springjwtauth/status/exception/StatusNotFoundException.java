package com.loggingsystem.springjwtauth.status.exception;

import com.loggingsystem.springjwtauth.exception.ErrorMessages;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class StatusNotFoundException extends RuntimeException {
    public StatusNotFoundException() {

        super(ErrorMessages.STATUS_NOT_FOUND.getMessage());
    }
}
