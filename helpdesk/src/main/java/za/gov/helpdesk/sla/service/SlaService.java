package za.gov.helpdesk.sla.service;

import za.gov.helpdesk.sla.dto.TicketSlaResponse;
import za.gov.helpdesk.sla.model.TicketSla;
import za.gov.helpdesk.ticket.model.Ticket;

public interface SlaService {

    TicketSla initializeSla(Ticket ticket);

    void recordFirstResponse(Long ticketId);

    void recordResolution(Long ticketId);

    TicketSlaResponse getSlaStatus(Long ticketId);
}
