package za.gov.helpdesk.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an operational block attempts to register or save a resource identifier
 * matrix that already exists in the persistent backend database.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    /**
     * Constructs an exception with a specific error detail message.
     *
     * @final final String message describing the duplication conflict context
     */
    public DuplicateResourceException(final String message) {
        super(message);
    }

    /**
     * Constructs an exception preserving the underlying system root cause. Crucial for satisfying
     * PMD-7's PreserveStackTrace verification rules.
     *
     * @final final String message describing the duplication conflict context
     * @final final Throwable cause the underlying exception (e.g., DataIntegrityViolationException)
     */
    public DuplicateResourceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
