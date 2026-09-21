package za.gov.helpdesk.knowledgebase.dto.response;

import java.time.LocalDateTime;

import za.gov.helpdesk.ticket.model.Status;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** One entry in an article's usage history - which ticket it was linked to resolve, and when. */
@Setter
@Getter
@Builder
public class ArticleTicketHistoryResponse {
    private Long ticketId;
    private String subject;
    private Status status;
    private LocalDateTime linkedAt;
}
