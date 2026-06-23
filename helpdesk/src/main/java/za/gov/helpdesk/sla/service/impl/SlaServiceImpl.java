package za.gov.helpdesk.sla.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.sla.dto.TicketSlaResponse;
import za.gov.helpdesk.sla.model.SlaPolicy;
import za.gov.helpdesk.sla.model.TicketSla;
import za.gov.helpdesk.sla.repository.SlaPolicyRepository;
import za.gov.helpdesk.sla.repository.TicketSlaRepository;
import za.gov.helpdesk.sla.service.BusinessHoursCalculator;
import za.gov.helpdesk.sla.service.SlaQueryHelper;
import za.gov.helpdesk.sla.service.SlaService;
import za.gov.helpdesk.ticket.model.Ticket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlaServiceImpl implements SlaService {

    private static final int REMINDER = 30;

    private final TicketSlaRepository ticketSlaRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final SlaQueryHelper slaQuery;
    private final BusinessHoursCalculator calculator;

    @Override
    @Transactional
    public TicketSla initializeSla(final Ticket ticket) {
        final SlaPolicy policy = slaQuery.getPolicyOrThrow(ticket.getPriority());

        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime responseDue =
                calculator.addBusinessMinutes(now, policy.getResponseMinutes());
        final LocalDateTime resolutionDue =
                calculator.addBusinessMinutes(now, policy.getResolutionMinutes());

        final TicketSla sla =
                TicketSla.builder()
                        .ticket(ticket)
                        .responseDueAt(responseDue)
                        .resolutionDueAt(resolutionDue)
                        .build();

        log.info(
                "SLA initialized: ticket={} responseDue={} resolutionDue={}",
                ticket.getId(),
                responseDue,
                resolutionDue);

        return ticketSlaRepository.save(sla);
    }

    @Override
    @Transactional
    public void recordFirstResponse(final Long ticketId) {
        ticketSlaRepository
                .findByTicketId(ticketId)
                .ifPresent(
                        sla -> {
                            if (sla.getFirstResponseAt() == null) {
                                sla.setFirstResponseAt(LocalDateTime.now());
                                sla.setResponseBreached(
                                        LocalDateTime.now().isAfter(sla.getResponseDueAt()));
                                ticketSlaRepository.save(sla);
                                log.info(
                                        "First response recorded: ticket={} breached={}",
                                        ticketId,
                                        sla.isResponseBreached());
                            }
                        });
    }

    @Override
    @Transactional
    public void recordResolution(final Long ticketId) {
        ticketSlaRepository
                .findByTicketId(ticketId)
                .ifPresent(
                        sla -> {
                            if (sla.getResolvedAt() == null) {
                                sla.setResolvedAt(LocalDateTime.now());
                                sla.setResolutionBreached(
                                        LocalDateTime.now().isAfter(sla.getResolutionDueAt()));
                                ticketSlaRepository.save(sla);
                                log.info(
                                        "Resolution recorded: ticket={} breached={}",
                                        ticketId,
                                        sla.isResolutionBreached());
                            }
                        });
    }

    @Override
    @Transactional(readOnly = true)
    public TicketSlaResponse getSlaStatus(final Long ticketId) {
        final TicketSla sla = slaQuery.findByTicketOrThrow(ticketId);

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

    private String computeStatus(final TicketSla sla) {
        if (sla.isResolutionBreached()) {
            return "BREACHED";
        }

        final int warningMinutes =
                slaPolicyRepository
                        .findByPriority(sla.getTicket().getPriority())
                        .map(SlaPolicy::getWarningThresholdMinutes)
                        .orElse(REMINDER);

        if (sla.getResolutionDueAt().minusMinutes(warningMinutes).isBefore(LocalDateTime.now())) {
            return "AT_RISK";
        }

        return "ON_TRACK";
    }
}
