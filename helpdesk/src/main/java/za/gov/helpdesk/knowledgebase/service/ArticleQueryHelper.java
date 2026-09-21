package za.gov.helpdesk.knowledgebase.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.knowledgebase.model.KnowledgeArticle;
import za.gov.helpdesk.knowledgebase.repository.KnowledgeArticleRepository;
import za.gov.helpdesk.users.model.Role;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;

/**
 * Shared article lookups, including the visibility rule every read path needs to respect: citizens
 * (role USER) may only ever see PUBLISHED articles - drafts and archived content are an internal,
 * staff-only view into the KB's editorial pipeline.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleQueryHelper {

    private final KnowledgeArticleRepository articleRepository;

    /**
     * Fetches an article by id with no visibility check, for internal callers that have already
     * established the actor's rights.
     *
     * @param articleId the article's id
     * @return the matching article
     * @throws ResourceNotFoundException if no article has that id
     */
    public KnowledgeArticle findOrThrow(final Long articleId) {
        return articleRepository
                .findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article", articleId));
    }

    /**
     * Fetches an article and enforces the citizen-visibility rule in one step, so a USER can't
     * discover a draft or archived article's existence via its id.
     */
    public KnowledgeArticle findVisibleOrThrow(final Long articleId, final User actor) {
        final KnowledgeArticle article = findOrThrow(articleId);
        assertVisible(article, actor);
        return article;
    }

    /**
     * Slug-based counterpart of {@link #findVisibleOrThrow}: fetches an article by its slug and
     * enforces the same citizen-visibility rule.
     *
     * @param slug the article's slug
     * @param actor the user requesting the article
     * @return the matching article, if the actor may see it
     * @throws ResourceNotFoundException if the slug is unknown or the article is hidden from the
     *     actor
     */
    public KnowledgeArticle findVisibleBySlugOrThrow(final String slug, final User actor) {
        final KnowledgeArticle article =
                articleRepository
                        .findBySlug(slug)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Article not found: " + slug));
        assertVisible(article, actor);
        return article;
    }

    private void assertVisible(final KnowledgeArticle article, final User actor) {
        final boolean isStaff = actor.getRole() == Role.AGENT || actor.getRole() == Role.ADMIN;
        if (!isStaff && article.getStatus() != KnowledgeArticle.ArticleStatus.PUBLISHED) {
            // A 404 rather than a 403 - a citizen shouldn't be able to tell the difference between
            // "doesn't exist" and "exists but you can't see it" for unpublished content.
            throw new ResourceNotFoundException("Article", article.getId());
        }
    }

    /**
     * For list/search endpoints: a USER's requested status filter is always overridden to
     * PUBLISHED.
     */
    public KnowledgeArticle.ArticleStatus resolveVisibleStatus(
            final KnowledgeArticle.ArticleStatus requested, final User actor) {
        final boolean isStaff = actor.getRole() == Role.AGENT || actor.getRole() == Role.ADMIN;
        if (!isStaff) {
            return KnowledgeArticle.ArticleStatus.PUBLISHED;
        }
        return requested;
    }
}
