package com.loggingsystem.springjwtauth.exception;

import lombok.Getter;

@Getter
public enum ErrorMessages {
    STATUS_NOT_FOUND("Status Not Found"),
    USER_ALREADY_EXISTS("User Already Exists"),
    USERNAME_NOT_FOUND("User Not Found With The Specified Email"),
    TICKET_NOT_FOUND("Ticket Not Found"),
    TECHNICIAN_NOT_AUTHORIZED_TO_UPDATE_TICKET("Technician is not authorized to update this ticket"),
    PRIORITY_NOT_FOUND("Priority Not Found"),
    CATEGORY_NOT_FOUND("Category Not Found");

    private final String message;

    ErrorMessages(String message) {
        this.message = message;
    }
}
