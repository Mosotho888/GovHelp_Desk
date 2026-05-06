package za.gov.helpdesk.category.exception;

import za.gov.helpdesk.exception.ErrorMessages;

public class CategoryAlreadyExistException extends RuntimeException{
    public CategoryAlreadyExistException() {
        super(ErrorMessages.CATEGORY_ALREADY_EXISTS.getMessage());
    }
}
