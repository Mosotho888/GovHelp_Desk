package za.gov.helpdesk.knowledgebase.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.exception.DuplicateResourceException;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.knowledgebase.dto.response.ArticleSummaryResponse;
import za.gov.helpdesk.knowledgebase.dto.response.ArticleTicketHistoryResponse;
import za.gov.helpdesk.knowledgebase.dto.response.TicketArticleLinkResponse;
import za.gov.helpdesk.knowledgebase.mapper.ArticleMapper;
import za.gov.helpdesk.knowledgebase.model.KnowledgeArticle;
import za.gov.helpdesk.knowledgebase.model.TicketKnowledgeArticle;
import za.gov.helpdesk.knowledgebase.repository.KnowledgeArticleRepository;
import za.gov.helpdesk.knowledgebase.repository.TicketKnowledgeArticleRepository;
import za.gov.helpdesk.knowledgebase.service.ArticleQueryHelper;
import za.gov.helpdesk.knowledgebase.service.TicketArticleLinkService;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.service.TicketQueryHelper;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketArticleLinkServiceImpl implements TicketArticleLinkService {

    private final TicketKnowledgeArticleRepository ticketArticleRepository;
    private final KnowledgeArticleRepository articleRepository;
    private final TicketQueryHelper ticketQuery;
    private final ArticleQueryHelper articleQuery;
    private final ArticleMapper articleMapper;
    private final AuditEventPublisher auditPublisher;

    @Override
    @Transactional
    public TicketArticleLinkResponse linkArticleToTicket(
            final Long ticketId, final Long articleId, final User actor) {
        final Ticket ticket = ticketQuery.findOrThrow(ticketId, actor);
        final KnowledgeArticle article = articleQuery.findOrThrow(articleId);

        if (ticketArticleRepository.existsByTicketIdAndArticleId(ticketId, articleId)) {
            throw new DuplicateResourceException(
                    "Article '" + article.getTitle() + "' is already linked to this ticket");
        }

        final TicketKnowledgeArticle link =
                ticketArticleRepository.save(
                        TicketKnowledgeArticle.builder()
                                .ticket(ticket)
                                .article(article)
                                .linkedById(actor.getId())
                                .linkedByName(actor.getName())
                                .build());

        article.setUsageCount(article.getUsageCount() + 1);
        articleRepository.save(article);

        final String description =
                "Article '"
                        + article.getTitle()
                        + "' linked to ticket #"
                        + ticketId
                        + " as the resolution";

        // Logged against both entities: discoverable from the ticket's own audit trail, and from
        // the article's, so "which tickets did this article actually help resolve" is answerable.
        auditPublisher.publishAudit(
                AuditLog.EntityType.TICKET,
                ticketId,
                actor,
                AuditLog.AuditAction.KB_ARTICLE_LINKED_TO_TICKET,
                null,
                article.getTitle(),
                description);
        auditPublisher.publishAudit(
                AuditLog.EntityType.KNOWLEDGE_ARTICLE,
                articleId,
                actor,
                AuditLog.AuditAction.KB_ARTICLE_LINKED_TO_TICKET,
                null,
                "Ticket #" + ticketId,
                description);

        return TicketArticleLinkResponse.builder()
                .ticketId(ticketId)
                .articleId(articleId)
                .articleTitle(article.getTitle())
                .linkedByName(link.getLinkedByName())
                .linkedAt(link.getLinkedAt())
                .build();
    }

    @Override
    @Transactional
    public void unlinkArticleFromTicket(
            final Long ticketId, final Long articleId, final User actor) {
        ticketQuery.findOrThrow(ticketId, actor);

        final TicketKnowledgeArticle link =
                ticketArticleRepository
                        .findByTicketIdAndArticleId(ticketId, articleId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Article "
                                                        + articleId
                                                        + " is not linked to ticket "
                                                        + ticketId));

        final String articleTitle = link.getArticle().getTitle();
        ticketArticleRepository.delete(link);

        final String description =
                "Article '" + articleTitle + "' unlinked from ticket #" + ticketId;
        auditPublisher.publishAudit(
                AuditLog.EntityType.TICKET,
                ticketId,
                actor,
                AuditLog.AuditAction.KB_ARTICLE_UNLINKED_FROM_TICKET,
                articleTitle,
                null,
                description);
        auditPublisher.publishAudit(
                AuditLog.EntityType.KNOWLEDGE_ARTICLE,
                articleId,
                actor,
                AuditLog.AuditAction.KB_ARTICLE_UNLINKED_FROM_TICKET,
                "Ticket #" + ticketId,
                null,
                description);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleSummaryResponse> getArticlesForTicket(
            final Long ticketId, final User actor) {
        ticketQuery.findOrThrow(ticketId, actor);

        return ticketArticleRepository.findByTicketIdOrderByLinkedAtDesc(ticketId).stream()
                .map(TicketKnowledgeArticle::getArticle)
                .map(articleMapper::toSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleTicketHistoryResponse> getTicketHistoryForArticle(
            final Long articleId, final Pageable pageable) {
        articleQuery.findOrThrow(articleId);

        return ticketArticleRepository
                .findByArticleIdOrderByLinkedAtDesc(articleId, pageable)
                .map(
                        link -> {
                            final Ticket ticket = link.getTicket();
                            return ArticleTicketHistoryResponse.builder()
                                    .ticketId(ticket.getId())
                                    .subject(ticket.getSubject())
                                    .status(ticket.getStatus())
                                    .linkedAt(link.getLinkedAt())
                                    .build();
                        });
    }
}
