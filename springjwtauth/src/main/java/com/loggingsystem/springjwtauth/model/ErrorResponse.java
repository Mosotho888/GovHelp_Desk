package com.loggingsystem.springjwtauth.model;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
public class ErrorResponse {
    private final LocalDateTime timestamp;
    private final String errCode;
    private final String message;
    private final int httpStatusCode;
    private final String httpStatus;
    private final String path;

    public ErrorResponse(String errCode, String message, HttpStatus httpStatus, String path) {
        this.timestamp = LocalDateTime.now();
        this.errCode = errCode;
        this.message = message;
        this.httpStatusCode = httpStatus.value();
        this.httpStatus = httpStatus.getReasonPhrase();
        this.path = path;
    }
}
