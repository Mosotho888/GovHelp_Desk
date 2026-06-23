package za.gov.helpdesk.exception;

/**
 * Exception thrown when an inbound network route or user session profile breaches the maximum
 * authorized traffic throttling allowances.
 */
public class RateLimitExceededException extends RuntimeException {
    /**
     * Constructs a new rate limit exception with a standard descriptive payload summary.
     *
     * @param message the detailed tracking message to log
     */
    public RateLimitExceededException(final String message) {
        super(message);
    }
}
