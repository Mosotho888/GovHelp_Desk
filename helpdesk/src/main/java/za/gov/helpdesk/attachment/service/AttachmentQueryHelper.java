package za.gov.helpdesk.attachment.service;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.attachment.model.Attachment;
import za.gov.helpdesk.attachment.repository.AttachmentRepository;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.ticket.service.TicketQueryHelper;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;

/**
 * Utility query component providing secure lookup strategies for ticket attachments. Interoperates
 * closely with the Ticket security framework to evaluate contextual access constraints before
 * exposing persistence layer entity details.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttachmentQueryHelper {

    private final AttachmentRepository attachmentRepository;
    private final TicketQueryHelper ticketQueryHelper;

    /**
     * Locates a file attachment record by its unique identifier and performs structural tenancy
     * ownership validation against the requesting security actor principal context.
     *
     * @param attachmentId the unique identifier of the target attachment
     * @param actor the authenticated {@link User} performing the data search operation
     * @return the verified {@link Attachment} entity matching the provided criteria
     * @throws ResourceNotFoundException if the attachment record does not exist
     */
    public Attachment findOrThrow(final Long attachmentId, final User actor) {
        final Attachment attachment =
                attachmentRepository
                        .findById(attachmentId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Attachment", attachmentId));

        // Delegate security context validation directly to the Ticket framework
        ticketQueryHelper.findOrThrow(attachment.getTicket().getId(), actor);

        return attachment;
    }

    /**
     * Resolves and extracts all files bound structurally to a specific ticket system container.
     * Guarantees parent container security validation boundaries pass before querying records.
     *
     * @param ticketId the unique target ticket identifier holding the attachments
     * @param actor the authenticated {@link User} executing the listing request
     * @return a {@link List} containing all verified {@link Attachment} records linked to the
     *     ticket
     * @throws ResourceNotFoundException if the underlying parent ticket entity is not found
     */
    public List<Attachment> findByTicketIdSecurely(final Long ticketId, final User actor) {
        // Enforce parent entity containment validation boundaries
        ticketQueryHelper.findOrThrow(ticketId, actor);

        return attachmentRepository.findByTicketId(ticketId);
    }
}
