package za.gov.helpdesk.notification.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;

@Component
@Getter
public class NotificationMetrics {

    private final Counter emailSent;
    private final Counter emailFailed;
    private final Counter emailDlq;
    private final Counter auditSaved;
    private final Counter auditFailed;
    private final Counter auditDlq;

    public NotificationMetrics(final MeterRegistry registry) {

        this.emailSent =
                Counter.builder("helpdesk.notification.email.sent")
                        .description("Emails successfully delivered via SMTP (consumer ACKed)")
                        .register(registry);

        this.emailFailed =
                Counter.builder("helpdesk.notification.email.failed")
                        .description("Email delivery failures that were NACKed and re-queued")
                        .register(registry);

        this.emailDlq =
                Counter.builder("helpdesk.notification.email.dlq")
                        .description(
                                "Email messages routed to the dead-letter queue (unrecoverable)")
                        .register(registry);

        this.auditSaved =
                Counter.builder("helpdesk.notification.audit.saved")
                        .description(
                                "Audit log entries successfully persisted by the audit consumer")
                        .register(registry);

        this.auditFailed =
                Counter.builder("helpdesk.notification.audit.failed")
                        .description("Audit log persistence failures in the audit consumer")
                        .register(registry);

        this.auditDlq =
                Counter.builder("helpdesk.notification.audit.dlq")
                        .description("Audit log routed to the dead-letter queue (unrecoverable)")
                        .register(registry);
    }

    public void incrementEmailSent() {
        this.emailSent.increment();
    }

    public void incrementEmailFailed() {
        this.emailFailed.increment();
    }

    public void incrementEmailDlq() {
        this.emailDlq.increment();
    }

    public void incrementAuditSaved() {
        this.auditSaved.increment();
    }

    public void incrementAuditFailed() {
        this.auditFailed.increment();
    }

    public void incrementAuditDlq() {
        this.auditDlq.increment();
    }
}
