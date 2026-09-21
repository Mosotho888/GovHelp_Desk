package za.gov.helpdesk.knowledgebase.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown for article lifecycle operations that don't make sense, e.g. archiving a draft directly
 * without ever publishing it, if that path is ever disallowed by policy.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidArticleOperationException extends RuntimeException {

    public InvalidArticleOperationException(final String message) {
        super(message);
    }
}
