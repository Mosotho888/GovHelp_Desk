package za.gov.helpdesk.knowledgebase.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import za.gov.helpdesk.knowledgebase.dto.response.ArticleSummaryResponse;
import za.gov.helpdesk.knowledgebase.dto.response.ArticleTicketHistoryResponse;
import za.gov.helpdesk.knowledgebase.dto.response.TicketArticleLinkResponse;
import za.gov.helpdesk.users.model.User;

public interface TicketArticleLinkService {

    TicketArticleLinkResponse linkArticleToTicket(Long ticketId, Long articleId, User actor);

    void unlinkArticleFromTicket(Long ticketId, Long articleId, User actor);

    /**
     * Articles linked to a ticket - open to anyone who can see the ticket, citizens included, so
     * they can see what self-service content resolved their own request.
     */
    List<ArticleSummaryResponse> getArticlesForTicket(Long ticketId, User actor);

    /** An article's usage history: every ticket it has ever been linked to, most recent first. */
    Page<ArticleTicketHistoryResponse> getTicketHistoryForArticle(
            Long articleId, Pageable pageable);
}
