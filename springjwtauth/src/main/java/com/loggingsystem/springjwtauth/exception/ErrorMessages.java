package com.loggingsystem.springjwtauth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorMessages {
    STATUS_NOT_FOUND("ERR_404_STATUS", "Status Not Found", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("ERR_409_USER", "User Already Exists", HttpStatus.CONFLICT),
    USERNAME_NOT_FOUND("ERR_404_USER", "User Not Found With The Specified Email", HttpStatus.NOT_FOUND),
    TICKET_NOT_FOUND("ERR_404_TICKET", "Ticket Not Found", HttpStatus.NOT_FOUND),
    TECHNICIAN_NOT_AUTHORIZED_TO_UPDATE_TICKET("ERR_403_TECHNICIAN", "Technician is not authorized to update this ticket", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorMessages(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
