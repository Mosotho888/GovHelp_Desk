package za.gov.helpdesk.notification.service.sla.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.gov.helpdesk.notification.service.MailSenderHelper;
import za.gov.helpdesk.notification.service.sla.SlaEmailService;
import za.gov.helpdesk.notification.service.sla.SlaEmailTemplateService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SlaEmailServiceImpl implements SlaEmailService {

    private final SlaEmailTemplateService templates;
    private final MailSenderHelper mailer;

    @Override
    public void sendSlaWarning(String to, String agentName, String ticketNumber, String subject, String deadlineType, LocalDateTime dueAt) {
        mailer.send(to,
                "⚠️ SLA Warning - " + deadlineType + " deadline approaching: " + ticketNumber,
                templates.slaWarning(ticketNumber, subject, deadlineType, dueAt, agentName));
    }

    @Override
    public void sendSlaBreach(String to, String agentName, String ticketNumber, String subject, String deadlineType) {
        mailer.send(to,
                "🚨 SLA Breached - " + deadlineType + ": " + ticketNumber,
                templates.slaBreach(ticketNumber, subject, deadlineType, agentName));
    }
}
