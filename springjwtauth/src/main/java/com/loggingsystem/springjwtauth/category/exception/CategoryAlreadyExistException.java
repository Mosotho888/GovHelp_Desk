package com.loggingsystem.springjwtauth.category.exception;

import com.loggingsystem.springjwtauth.exception.ErrorMessages;

public class CategoryAlreadyExistException extends RuntimeException{
    public CategoryAlreadyExistException() {
        super(ErrorMessages.CATEGORY_ALREADY_EXISTS.getMessage());
    }
}
