package za.gov.helpdesk.notification.service.sla.impl;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.gov.helpdesk.notification.service.EmailTemplateRenderer;
import za.gov.helpdesk.notification.service.MailSenderHelper;
import za.gov.helpdesk.notification.service.sla.SlaEmailService;

@Service
@RequiredArgsConstructor
public class SlaEmailServiceImpl implements SlaEmailService {

    private static final String PREFIX = "email/sla/";

    private final EmailTemplateRenderer renderer;
    private final MailSenderHelper mailer;

    @Override
    public void sendSlaWarning(String to, String agentName, String ticketNumber, String subject, String deadlineType,
            LocalDateTime dueAt) {
        Map<String, Object> model = Map.of("agentName", agentName, "ticketNumber", ticketNumber, "subject", subject,
                "deadlineType", deadlineType, "dueAt", dueAt);

        mailer.send(to, "SLA Warning - " + deadlineType + " deadline approaching: " + ticketNumber,
                renderer.render(PREFIX + "sla-warning", model));
    }

    @Override
    public void sendSlaBreach(String to, String agentName, String ticketNumber, String subject, String deadlineType) {
        Map<String, Object> model = Map.of("agentName", agentName, "ticketNumber", ticketNumber, "subject", subject,
                "deadlineType", deadlineType);

        mailer.send(to, "SLA Breached - " + deadlineType + ": " + ticketNumber,
                renderer.render(PREFIX + "sla-breach", model));
    }
}
