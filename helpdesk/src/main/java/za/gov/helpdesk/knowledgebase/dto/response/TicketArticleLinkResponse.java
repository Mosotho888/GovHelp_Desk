package za.gov.helpdesk.knowledgebase.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class TicketArticleLinkResponse {
    private Long ticketId;
    private Long articleId;
    private String articleTitle;
    private String linkedByName;
    private LocalDateTime linkedAt;
}
