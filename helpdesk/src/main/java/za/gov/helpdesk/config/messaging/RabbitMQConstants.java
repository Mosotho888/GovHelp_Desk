package za.gov.helpdesk.config.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@NoArgsConstructor
@AllArgsConstructor
public final class RabbitMQConstants {

    public static final String EXCHANGE = "helpdesk.exchange";
    public static final String DLX = "helpdesk.dlx";

    public static final String AUDIT_QUEUE = "audit.queue";
    public static final String EMAIL_QUEUE = "email.queue";
    public static final String AUDIT_DLQ = "audit.dlq";
    public static final String EMAIL_DLQ = "email.dlq";

    public static final String AUDIT_ROUTING_KEY = "audit.#";
    public static final String EMAIL_ROUTING_KEY = "email.#";
    public static final String AUDIT_DLQ_ROUTING_KEY = "failed.audit.#";
    public static final String EMAIL_DLQ_ROUTING_KEY = "failed.email.#";
}
