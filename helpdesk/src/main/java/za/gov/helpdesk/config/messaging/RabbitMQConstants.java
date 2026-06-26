package za.gov.helpdesk.config.messaging;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class RabbitMQConstants {

    public static final String EXCHANGE = "helpdesk.exchange";
    public static final String DLX = "helpdesk.dlx";

    public static final String AUDIT_QUEUE = "audit.queue";
    public static final String TICKET_EMAIL_QUEUE = "ticket.email.queue";
    public static final String PASSWORD_RESET_EMAIL_QUEUE = "password.reset.email.queue";
    public static final String SLA_EMAIL_QUEUE = "sla.email.queue";
    public static final String AUDIT_DLQ = "audit.dlq";
    public static final String TICKET_EMAIL_DLQ = "ticket.email.dlq";
    public static final String PASSWORD_RESET_EMAIL_DLQ = "password.reset.email.dlq";
    public static final String SLA_EMAIL_DLQ = "sla.email.dlq";

    public static final String AUDIT_ROUTING_KEY = "audit.#";
    public static final String TICKET_EMAIL_ROUTING_KEY = "ticket.email.#";
    public static final String PASSWORD_RESET_EMAIL_ROUTING_KEY = "password.reset.email.#";
    public static final String SLA_EMAIL_ROUTING_KEY = "sla.email.#";

    public static final String AUDIT_DLQ_ROUTING_KEY = "failed.audit.#";
    public static final String TICKET_EMAIL_DLQ_ROUTING_KEY = "failed.ticket.email.#";
    public static final String PASSWORD_RESET_EMAIL_DLQ_ROUTING_KEY =
            "failed.password.reset.email.#";
    public static final String SLA_EMAIL_DLQ_ROUTING_KEY = "failed.sla.email.#";
}
