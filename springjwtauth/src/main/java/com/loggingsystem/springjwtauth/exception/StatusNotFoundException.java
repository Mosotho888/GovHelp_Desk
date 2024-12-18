package com.loggingsystem.springjwtauth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class StatusNotFoundException extends CustomException {
    public StatusNotFoundException() {
        super(ErrorMessages.STATUS_NOT_FOUND);
    }
}
