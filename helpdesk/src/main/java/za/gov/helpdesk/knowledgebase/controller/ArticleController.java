package za.gov.helpdesk.knowledgebase.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import za.gov.helpdesk.knowledgebase.dto.request.ArticleFeedbackRequest;
import za.gov.helpdesk.knowledgebase.dto.request.CreateArticleRequest;
import za.gov.helpdesk.knowledgebase.dto.request.UpdateArticleRequest;
import za.gov.helpdesk.knowledgebase.dto.response.ArticleFeedbackResponse;
import za.gov.helpdesk.knowledgebase.dto.response.ArticleResponse;
import za.gov.helpdesk.knowledgebase.dto.response.ArticleSummaryResponse;
import za.gov.helpdesk.knowledgebase.dto.response.ArticleTicketHistoryResponse;
import za.gov.helpdesk.knowledgebase.model.KnowledgeArticle;
import za.gov.helpdesk.knowledgebase.service.ArticleService;
import za.gov.helpdesk.knowledgebase.service.TicketArticleLinkService;
import za.gov.helpdesk.users.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@SuppressWarnings("checkstyle:ClassFanOutComplexity")
@RestController
@RequestMapping("/v1/knowledge-base")
@RequiredArgsConstructor
@Tag(name = "Knowledge Base", description = "Self-service troubleshooting guides, FAQs, and SOPs")
@SecurityRequirement(name = "bearerAuth")
public class ArticleController {

    private final ArticleService articleService;
    private final TicketArticleLinkService ticketArticleLinkService;

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Draft a new article (Agent/Admin only)")
    public ResponseEntity<ArticleResponse> createArticle(
            @Valid @RequestBody final CreateArticleRequest request,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(articleService.createArticle(request, principal.getUser()));
    }

    @GetMapping
    @Operation(
            summary = "Browse articles",
            description =
                    "Filterable by status, type, categoryId, and tag. Citizens (role USER) only"
                        + " ever see PUBLISHED articles regardless of the status filter requested.")
    public ResponseEntity<Page<ArticleSummaryResponse>> getArticles(
            @RequestParam(required = false) final KnowledgeArticle.ArticleStatus status,
            @RequestParam(required = false) final KnowledgeArticle.ArticleType type,
            @RequestParam(required = false) final Long categoryId,
            @RequestParam(required = false) final String tag,
            @PageableDefault(size = 20, sort = "updatedAt") final Pageable pageable,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.ok(
                articleService.getArticles(
                        status, type, categoryId, tag, pageable, principal.getUser()));
    }

    @GetMapping("/search")
    @Operation(summary = "Full-text search across title, summary, and content, ranked by relevance")
    public ResponseEntity<Page<ArticleSummaryResponse>> searchArticles(
            @RequestParam final String q,
            @PageableDefault(size = 20) final Pageable pageable,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.ok(articleService.searchArticles(q, pageable, principal.getUser()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an article by ID (increments its view count)")
    public ResponseEntity<ArticleResponse> getArticleById(
            @PathVariable final Long id,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.ok(articleService.getArticleById(id, principal.getUser()));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get an article by its slug (for friendly URLs)")
    public ResponseEntity<ArticleResponse> getArticleBySlug(
            @PathVariable final String slug,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.ok(articleService.getArticleBySlug(slug, principal.getUser()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(
            summary =
                    "Edit an article's content, category, tags, or lifecycle status (Agent/Admin"
                            + " only)")
    public ResponseEntity<ArticleResponse> updateArticle(
            @PathVariable final Long id,
            @Valid @RequestBody final UpdateArticleRequest request,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.ok(articleService.updateArticle(id, request, principal.getUser()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Permanently delete an article (Admin only). Prefer archiving via PATCH.")
    public void deleteArticle(
            @PathVariable final Long id,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        articleService.deleteArticle(id, principal.getUser());
    }

    @PostMapping("/{id}/feedback")
    @Operation(summary = "Rate an article helpful or not helpful (one vote per user, changeable)")
    public ResponseEntity<ArticleFeedbackResponse> submitFeedback(
            @PathVariable final Long id,
            @Valid @RequestBody final ArticleFeedbackRequest request,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.ok(articleService.submitFeedback(id, request, principal.getUser()));
    }

    @GetMapping("/{id}/feedback")
    @Operation(summary = "Get an article's feedback totals and the requester's own vote, if any")
    public ResponseEntity<ArticleFeedbackResponse> getFeedback(
            @PathVariable final Long id,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.ok(articleService.getFeedback(id, principal.getUser()));
    }

    @GetMapping("/{id}/tickets")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Get an article's usage history: every ticket it has helped resolve")
    public ResponseEntity<Page<ArticleTicketHistoryResponse>> getArticleTicketHistory(
            @PathVariable final Long id, @PageableDefault(size = 25) final Pageable pageable) {
        return ResponseEntity.ok(ticketArticleLinkService.getTicketHistoryForArticle(id, pageable));
    }
}
