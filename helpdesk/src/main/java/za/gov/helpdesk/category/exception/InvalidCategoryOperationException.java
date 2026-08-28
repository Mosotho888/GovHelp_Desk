package za.gov.helpdesk.category.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a category mutation would violate the shape of the category tree: exceeding the
 * maximum nesting depth, introducing a cycle, or deactivating a category that still has active
 * tickets or subcategories attached to it.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidCategoryOperationException extends RuntimeException {

    public InvalidCategoryOperationException(String message) {
        super(message);
    }
}
