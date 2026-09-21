package za.gov.helpdesk.knowledgebase.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import za.gov.helpdesk.knowledgebase.dto.response.ArticleSummaryResponse;
import za.gov.helpdesk.knowledgebase.dto.response.TicketArticleLinkResponse;
import za.gov.helpdesk.knowledgebase.service.TicketArticleLinkService;
import za.gov.helpdesk.users.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Links tickets to the knowledge base articles that resolved them. Kept as a separate controller
 * from {@code TicketController}, mirroring {@code TicketAssetController}'s nested-path pattern.
 */
@RestController
@RequiredArgsConstructor
@Tag(
        name = "Ticket Knowledge Articles",
        description = "Associating tickets with the KB articles that resolved them")
@SecurityRequirement(name = "bearerAuth")
public class TicketArticleController {

    private final TicketArticleLinkService ticketArticleLinkService;

    /**
     * Records that an article helped resolve a ticket.
     *
     * @param ticketId the ticket being resolved
     * @param articleId the article that helped
     * @param principal the authenticated agent or admin
     * @return the created link, with 201 Created
     */
    @PostMapping("/v1/tickets/{ticketId}/knowledge-articles/{articleId}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Link an article to a ticket as (part of) its resolution")
    public ResponseEntity<TicketArticleLinkResponse> linkArticle(
            @PathVariable final Long ticketId,
            @PathVariable final Long articleId,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ticketArticleLinkService.linkArticleToTicket(
                                ticketId, articleId, principal.getUser()));
    }

    @DeleteMapping("/v1/tickets/{ticketId}/knowledge-articles/{articleId}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unlink an article from a ticket")
    public void unlinkArticle(
            @PathVariable final Long ticketId,
            @PathVariable final Long articleId,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        ticketArticleLinkService.unlinkArticleFromTicket(ticketId, articleId, principal.getUser());
    }

    @GetMapping("/v1/tickets/{ticketId}/knowledge-articles")
    @Operation(
            summary =
                    "List the articles linked to a ticket - visible to the ticket's own requester"
                            + " too")
    public ResponseEntity<List<ArticleSummaryResponse>> getArticlesForTicket(
            @PathVariable final Long ticketId,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.ok(
                ticketArticleLinkService.getArticlesForTicket(ticketId, principal.getUser()));
    }
}
