package za.gov.helpdesk.exception;

/**
 * Unchecked exception thrown when an outbound email fails to send because of an SMTP transport
 * failure or a MIME message construction error.
 */
public class EmailDeliveryException extends RuntimeException {

    /**
     * Constructs a new email delivery exception preserving the underlying root cause.
     *
     * @param message a message detailing the failure context.
     * @param cause the underlying transport or MIME exception.
     */
    public EmailDeliveryException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
