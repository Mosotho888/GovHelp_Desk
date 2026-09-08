package za.gov.helpdesk.notification.service.sla.impl;

import java.util.Map;

import org.springframework.stereotype.Service;

import za.gov.helpdesk.notification.dto.SlaEmailNotificationMessage;
import za.gov.helpdesk.notification.service.EmailTemplateRenderer;
import za.gov.helpdesk.notification.service.MailSenderHelper;
import za.gov.helpdesk.notification.service.sla.SlaEmailService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SlaEmailServiceImpl implements SlaEmailService {

    private static final String PREFIX = "email/sla/";

    private final EmailTemplateRenderer renderer;
    private final MailSenderHelper mailer;

    @Override
    public void sendSlaWarning(final SlaEmailNotificationMessage message) {
        final Map<String, Object> model =
                Map.of(
                        "agentName",
                        message.getAgentName(),
                        "ticketNumber",
                        message.getTicketNumber(),
                        "subject",
                        message.getTicketSubject(),
                        "deadlineType",
                        message.getDeadlineType(),
                        "dueAt",
                        message.getDueAt());

        mailer.send(
                message.getAgentEmail(),
                "SLA Warning - "
                        + message.getDeadlineType()
                        + " deadline approaching: "
                        + message.getTicketNumber(),
                renderer.render(PREFIX + "sla-warning", model));
    }

    @Override
    public void sendSlaBreach(final SlaEmailNotificationMessage message) {
        final Map<String, Object> model =
                Map.of(
                        "agentName",
                        message.getAgentName(),
                        "ticketNumber",
                        message.getTicketNumber(),
                        "subject",
                        message.getTicketSubject(),
                        "deadlineType",
                        message.getDeadlineType());

        mailer.send(
                message.getAgentEmail(),
                "SLA Breached - " + message.getDeadlineType() + ": " + message.getTicketNumber(),
                renderer.render(PREFIX + "sla-breach", model));
    }
}
