package com.loggingsystem.springjwtauth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException{
    private final String code;
    private final HttpStatus status;

    public CustomException(ErrorMessages errorMessages) {
        super(errorMessages.getMessage());
        this.code = errorMessages.getCode();
        this.status = errorMessages.getStatus();
    }
}
