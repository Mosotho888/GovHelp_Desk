package za.gov.helpdesk.sla.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.notification.service.sla.SlaEmailService;
import za.gov.helpdesk.sla.dto.TicketSlaResponse;
import za.gov.helpdesk.sla.model.SlaPolicy;
import za.gov.helpdesk.sla.model.TicketSla;
import za.gov.helpdesk.sla.repository.SlaPolicyRepository;
import za.gov.helpdesk.sla.repository.TicketSlaRepository;
import za.gov.helpdesk.sla.service.BusinessHoursCalculator;
import za.gov.helpdesk.sla.service.SlaService;
import za.gov.helpdesk.ticket.model.Ticket;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlaServiceImpl implements SlaService {

    private final TicketSlaRepository ticketSlaRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final BusinessHoursCalculator calculator;

    @Override
    @Transactional
    public TicketSla initializeSla(Ticket ticket) {
        SlaPolicy policy = slaPolicyRepository
                .findByPriority(ticket.getPriority())
                .orElseThrow(() -> new IllegalStateException(
                        "No SLA policy for priority: " + ticket.getPriority()));

        LocalDateTime now           = LocalDateTime.now();
        LocalDateTime responseDue   = calculator.addBusinessMinutes(now, policy.getResponseMinutes());
        LocalDateTime resolutionDue = calculator.addBusinessMinutes(now, policy.getResolutionMinutes());

        TicketSla sla = TicketSla.builder()
                .ticket(ticket)
                .responseDueAt(responseDue)
                .resolutionDueAt(resolutionDue)
                .build();

        log.info("SLA initialized: ticket={} responseDue={} resolutionDue={}",
                ticket.getId(), responseDue, resolutionDue);

        return ticketSlaRepository.save(sla);
    }

    @Override
    @Transactional
    public void recordFirstResponse(Long ticketId) {
        ticketSlaRepository.findByTicketId(ticketId).ifPresent(sla -> {
            if (sla.getFirstResponseAt() == null) {
                sla.setFirstResponseAt(LocalDateTime.now());
                sla.setResponseBreached(LocalDateTime.now().isAfter(sla.getResponseDueAt()));
                ticketSlaRepository.save(sla);
                log.info("First response recorded: ticket={} breached={}",
                        ticketId, sla.isResponseBreached());
            }
        });
    }

    @Override
    public void recordResolution(Long ticketId) {
        ticketSlaRepository.findByTicketId(ticketId).ifPresent(sla -> {
            if (sla.getResolvedAt() == null) {
                sla.setResolvedAt(LocalDateTime.now());
                sla.setResolutionBreached(
                        LocalDateTime.now().isAfter(sla.getResolutionDueAt()));
                ticketSlaRepository.save(sla);
                log.info("Resolution recorded: ticket={} breached={}",
                        ticketId, sla.isResolutionBreached());
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public TicketSlaResponse getSlaStatus(Long ticketId) {
        TicketSla sla = ticketSlaRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SLA for ticket", ticketId));

        return TicketSlaResponse.builder()
                .responseDueAt(sla.getResponseDueAt())
                .resolutionDueAt(sla.getResolutionDueAt())
                .firstResponseAt(sla.getFirstResponseAt())
                .resolvedAt(sla.getResolvedAt())
                .responseBreached(sla.isResponseBreached())
                .resolutionBreached(sla.isResolutionBreached())
                .status(computeStatus(sla))
                .build();
    }

    private String computeStatus(TicketSla sla) {
        if (sla.isResolutionBreached()) return "BREACHED";

        int warningMinutes = slaPolicyRepository
                .findByPriority(sla.getTicket().getPriority())
                .map(SlaPolicy::getWarningThresholdMinutes)
                .orElse(30);

        if (sla.getResolutionDueAt()
                .minusMinutes(warningMinutes)
                .isBefore(LocalDateTime.now())) {
            return "AT_RISK";
        }

        return "ON_TRACK";
    }
}
