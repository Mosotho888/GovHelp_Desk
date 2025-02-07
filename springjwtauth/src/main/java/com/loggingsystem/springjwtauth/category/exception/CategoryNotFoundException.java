package com.loggingsystem.springjwtauth.category.exception;

import com.loggingsystem.springjwtauth.exception.ErrorMessages;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CategoryNotFoundException extends RuntimeException{
    public CategoryNotFoundException() {
        super(ErrorMessages.CATEGORY_NOT_FOUND.getMessage());
    }
}
