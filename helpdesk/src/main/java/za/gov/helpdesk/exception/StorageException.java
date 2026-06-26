package za.gov.helpdesk.exception;

/**
 * Custom runtime exception thrown when physical disk storage operations or attachment processing
 * fails.
 */
public class StorageException extends RuntimeException {

    /**
     * Constructs a new storage exception with a specific business message.
     *
     * @final final string message detailing the failure context.
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * Constructs a new storage exception preserving the underlying root cause. Crucial for passing
     * PMD's PreserveStackTrace verification rules.
     *
     * @final final string message detailing the failure context.
     * @final final throwable cause of the underlying system exception.
     */
    public StorageException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
