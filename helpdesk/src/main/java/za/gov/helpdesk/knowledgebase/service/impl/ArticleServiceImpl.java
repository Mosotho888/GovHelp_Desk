package za.gov.helpdesk.knowledgebase.service.impl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.category.service.CategoryQueryHelper;
import za.gov.helpdesk.knowledgebase.dto.request.ArticleFeedbackRequest;
import za.gov.helpdesk.knowledgebase.dto.request.CreateArticleRequest;
import za.gov.helpdesk.knowledgebase.dto.request.UpdateArticleRequest;
import za.gov.helpdesk.knowledgebase.dto.response.ArticleFeedbackResponse;
import za.gov.helpdesk.knowledgebase.dto.response.ArticleResponse;
import za.gov.helpdesk.knowledgebase.dto.response.ArticleSummaryResponse;
import za.gov.helpdesk.knowledgebase.exception.InvalidArticleOperationException;
import za.gov.helpdesk.knowledgebase.mapper.ArticleMapper;
import za.gov.helpdesk.knowledgebase.model.KnowledgeArticle;
import za.gov.helpdesk.knowledgebase.model.KnowledgeArticleFeedback;
import za.gov.helpdesk.knowledgebase.repository.KnowledgeArticleFeedbackRepository;
import za.gov.helpdesk.knowledgebase.repository.KnowledgeArticleRepository;
import za.gov.helpdesk.knowledgebase.service.ArticleQueryHelper;
import za.gov.helpdesk.knowledgebase.service.ArticleService;
import za.gov.helpdesk.users.model.Role;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;

@SuppressWarnings({"checkstyle:ClassFanOutComplexity", "PMD.CouplingBetweenObjects"})
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final KnowledgeArticleRepository articleRepository;
    private final KnowledgeArticleFeedbackRepository feedbackRepository;
    private final ArticleQueryHelper articleQuery;
    private final CategoryQueryHelper categoryQuery;
    private final ArticleMapper articleMapper;
    private final AuditEventPublisher auditPublisher;

    @Override
    @Transactional
    public ArticleResponse createArticle(final CreateArticleRequest request, final User actor) {
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryQuery.findOrThrow(request.getCategoryId());
        }

        final KnowledgeArticle article =
                KnowledgeArticle.builder()
                        .title(request.getTitle())
                        .slug(generateUniqueSlug(request.getTitle()))
                        .summary(request.getSummary())
                        .content(request.getContent())
                        .type(request.getType())
                        .status(KnowledgeArticle.ArticleStatus.DRAFT)
                        .category(category)
                        .author(actor)
                        .tags(normaliseTags(request.getTags()))
                        .build();

        final KnowledgeArticle saved = articleRepository.save(article);

        auditPublisher.publishAudit(
                AuditLog.EntityType.KNOWLEDGE_ARTICLE,
                saved.getId(),
                actor,
                AuditLog.AuditAction.KB_ARTICLE_CREATED,
                null,
                saved.getTitle(),
                "Drafted article: " + saved.getTitle());

        return articleMapper.toArticleResponse(saved);
    }

    @Override
    @Transactional
    public ArticleResponse getArticleById(final Long articleId, final User actor) {
        final KnowledgeArticle article = articleQuery.findVisibleOrThrow(articleId, actor);
        articleRepository.incrementViewCount(articleId);
        return articleMapper.toArticleResponse(article);
    }

    @Override
    @Transactional
    public ArticleResponse getArticleBySlug(final String slug, final User actor) {
        final KnowledgeArticle article = articleQuery.findVisibleBySlugOrThrow(slug, actor);
        articleRepository.incrementViewCount(article.getId());
        return articleMapper.toArticleResponse(article);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleSummaryResponse> getArticles(
            final KnowledgeArticle.ArticleStatus status,
            final KnowledgeArticle.ArticleType type,
            final Long categoryId,
            final String tag,
            final Pageable pageable,
            final User actor) {
        final KnowledgeArticle.ArticleStatus resolvedStatus =
                articleQuery.resolveVisibleStatus(status, actor);
        final String normalizedTag = tag != null ? tag.trim().toLowerCase(Locale.ROOT) : null;

        return articleRepository
                .findWithFilters(resolvedStatus, type, categoryId, normalizedTag, pageable)
                .map(articleMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleSummaryResponse> searchArticles(
            final String query, final Pageable pageable, final User actor) {
        // Employee only ever search published content; staff can pass through to search drafts too
        // by not restricting the status (null means "any status" in the native query).
        final boolean isStaff = actor.getRole() == Role.AGENT || actor.getRole() == Role.ADMIN;
        final String statusFilter =
                isStaff ? null : KnowledgeArticle.ArticleStatus.PUBLISHED.name();

        return articleRepository
                .search(query, statusFilter, pageable)
                .map(articleMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public ArticleResponse updateArticle(
            final Long articleId, final UpdateArticleRequest request, final User actor) {
        final KnowledgeArticle article = articleQuery.findOrThrow(articleId);

        applyContentUpdates(article, request);
        applyCategoryUpdate(article, request);
        applyStatusTransition(article, request, actor);

        return articleMapper.toArticleResponse(articleRepository.save(article));
    }

    private void applyContentUpdates(
            final KnowledgeArticle article, final UpdateArticleRequest request) {
        if (request.getTitle() != null) {
            article.setTitle(request.getTitle());
        }
        if (request.getSummary() != null) {
            article.setSummary(request.getSummary());
        }
        if (request.getContent() != null) {
            article.setContent(request.getContent());
        }
        if (request.getType() != null) {
            article.setType(request.getType());
        }
        if (request.getTags() != null) {
            article.setTags(normaliseTags(request.getTags()));
        }
    }

    private void applyCategoryUpdate(
            final KnowledgeArticle article, final UpdateArticleRequest request) {
        if (request.isClearCategory()) {
            article.setCategory(null);
            return;
        }
        if (request.getCategoryId() != null) {
            article.setCategory(categoryQuery.findOrThrow(request.getCategoryId()));
        }
    }

    private void applyStatusTransition(
            final KnowledgeArticle article, final UpdateArticleRequest request, final User actor) {
        final KnowledgeArticle.ArticleStatus requested = request.getStatus();
        if (requested == null || requested == article.getStatus()) {
            return;
        }

        if (requested == KnowledgeArticle.ArticleStatus.DRAFT) {
            throw new InvalidArticleOperationException(
                    "An article that has been published can't be moved back to DRAFT - archive it"
                            + " instead if it should no longer be visible.");
        }

        final KnowledgeArticle.ArticleStatus previous = article.getStatus();
        article.setStatus(requested);

        if (requested == KnowledgeArticle.ArticleStatus.PUBLISHED) {
            if (article.getPublishedAt() == null) {
                article.setPublishedAt(LocalDateTime.now());
            }
            auditPublisher.publishAudit(
                    AuditLog.EntityType.KNOWLEDGE_ARTICLE,
                    article.getId(),
                    actor,
                    AuditLog.AuditAction.KB_ARTICLE_PUBLISHED,
                    previous.name(),
                    requested.name(),
                    null);
        } else {
            auditPublisher.publishAudit(
                    AuditLog.EntityType.KNOWLEDGE_ARTICLE,
                    article.getId(),
                    actor,
                    AuditLog.AuditAction.KB_ARTICLE_ARCHIVED,
                    previous.name(),
                    requested.name(),
                    null);
        }
    }

    @Override
    @Transactional
    public void deleteArticle(final Long articleId, final User actor) {
        final KnowledgeArticle article = articleQuery.findOrThrow(articleId);
        auditPublisher.publishAudit(
                AuditLog.EntityType.KNOWLEDGE_ARTICLE,
                articleId,
                actor,
                AuditLog.AuditAction.KB_ARTICLE_DELETED,
                article.getTitle(),
                null,
                null);
        articleRepository.delete(article);
    }

    @Override
    @Transactional
    public ArticleFeedbackResponse submitFeedback(
            final Long articleId, final ArticleFeedbackRequest request, final User actor) {
        // A citizen can only rate content they were actually allowed to read.
        final KnowledgeArticle article = articleQuery.findVisibleOrThrow(articleId, actor);
        final boolean helpful = Boolean.TRUE.equals(request.getHelpful());

        final Optional<KnowledgeArticleFeedback> existing =
                feedbackRepository.findByArticleIdAndUserId(articleId, actor.getId());
        if (existing.isPresent()) {
            final KnowledgeArticleFeedback feedback = existing.get();
            if (feedback.isHelpful() != helpful) {
                adjustCount(article, feedback.isHelpful(), -1);
                adjustCount(article, helpful, 1);
                feedback.setHelpful(helpful);
                feedbackRepository.save(feedback);
                articleRepository.save(article);
            }
        } else {
            feedbackRepository.save(
                    KnowledgeArticleFeedback.builder()
                            .article(article)
                            .user(actor)
                            .helpful(helpful)
                            .build());
            adjustCount(article, helpful, 1);
            articleRepository.save(article);
        }

        return buildFeedbackResponse(article, helpful);
    }

    private void adjustCount(
            final KnowledgeArticle article, final boolean helpful, final int delta) {
        if (helpful) {
            article.setHelpfulCount(article.getHelpfulCount() + delta);
        } else {
            article.setNotHelpfulCount(article.getNotHelpfulCount() + delta);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleFeedbackResponse getFeedback(final Long articleId, final User actor) {
        final KnowledgeArticle article = articleQuery.findVisibleOrThrow(articleId, actor);
        final Boolean yourVote =
                feedbackRepository
                        .findByArticleIdAndUserId(articleId, actor.getId())
                        .map(KnowledgeArticleFeedback::isHelpful)
                        .orElse(null);
        return buildFeedbackResponse(article, yourVote);
    }

    private ArticleFeedbackResponse buildFeedbackResponse(
            final KnowledgeArticle article, final Boolean yourVote) {
        return ArticleFeedbackResponse.builder()
                .articleId(article.getId())
                .helpfulCount(article.getHelpfulCount())
                .notHelpfulCount(article.getNotHelpfulCount())
                .yourVote(yourVote)
                .build();
    }

    private String generateUniqueSlug(final String title) {
        final String base =
                title.toLowerCase(Locale.ROOT)
                        .trim()
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("(^-|-$)", "");
        String candidate = base;
        int suffix = 2;
        while (articleRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private Set<String> normaliseTags(final Set<String> tags) {
        if (tags == null) {
            return new HashSet<>();
        }
        final Set<String> normalised = new HashSet<>();
        for (final String tag : tags) {
            if (tag != null && !tag.isBlank()) {
                normalised.add(tag.trim().toLowerCase(Locale.ROOT));
            }
        }
        return normalised;
    }
}
