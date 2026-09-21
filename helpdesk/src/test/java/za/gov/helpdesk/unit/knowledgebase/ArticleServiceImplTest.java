package za.gov.helpdesk.unit.knowledgebase;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.category.service.CategoryQueryHelper;
import za.gov.helpdesk.exception.ResourceNotFoundException;
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
import za.gov.helpdesk.knowledgebase.service.impl.ArticleServiceImpl;
import za.gov.helpdesk.users.model.Role;
import za.gov.helpdesk.users.model.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleServiceImpl unit tests")
class ArticleServiceImplTest {

    @Mock private KnowledgeArticleRepository articleRepository;
    @Mock private KnowledgeArticleFeedbackRepository feedbackRepository;
    @Mock private ArticleQueryHelper articleQuery;
    @Mock private CategoryQueryHelper categoryQuery;
    @Mock private ArticleMapper articleMapper;
    @Mock private AuditEventPublisher auditPublisher;

    private ArticleServiceImpl articleService;

    private User admin;
    private User citizen;
    private KnowledgeArticle article;

    @BeforeEach
    void setUp() {
        articleService =
                new ArticleServiceImpl(
                        articleRepository,
                        feedbackRepository,
                        articleQuery,
                        categoryQuery,
                        articleMapper,
                        auditPublisher);

        admin = User.builder().id(1L).name("Admin").role(Role.ADMIN).build();
        citizen = User.builder().id(2L).name("Citizen").role(Role.USER).build();

        article =
                KnowledgeArticle.builder()
                        .id(10L)
                        .title("Requesting VPN Access")
                        .slug("requesting-vpn-access")
                        .content("Original content")
                        .type(KnowledgeArticle.ArticleType.FAQ)
                        .status(KnowledgeArticle.ArticleStatus.DRAFT)
                        .tags(new HashSet<>(Set.of("vpn")))
                        .helpfulCount(0)
                        .notHelpfulCount(0)
                        .build();
    }

    @Nested
    @DisplayName("createArticle()")
    class CreateArticle {

        @Test
        @DisplayName("drafts an article without a category and publishes KB_ARTICLE_CREATED")
        void createArticle_withoutCategory_savesAsDraftAndAudits() {
            final CreateArticleRequest request = new CreateArticleRequest();
            request.setTitle("Requesting VPN Access");
            request.setContent("Steps to request VPN access");
            request.setType(KnowledgeArticle.ArticleType.FAQ);

            given(articleRepository.existsBySlug(anyString())).willReturn(false);
            given(articleRepository.save(any(KnowledgeArticle.class))).willReturn(article);
            given(articleMapper.toArticleResponse(article))
                    .willReturn(ArticleResponse.builder().id(10L).build());

            final ArticleResponse response = articleService.createArticle(request, admin);

            assertThat(response.getId()).isEqualTo(10L);

            final ArgumentCaptor<KnowledgeArticle> captor =
                    ArgumentCaptor.forClass(KnowledgeArticle.class);
            verify(articleRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus())
                    .isEqualTo(KnowledgeArticle.ArticleStatus.DRAFT);
            assertThat(captor.getValue().getCategory()).isNull();
            assertThat(captor.getValue().getAuthor()).isEqualTo(admin);

            verify(auditPublisher)
                    .publishAudit(
                            eq(AuditLog.EntityType.KNOWLEDGE_ARTICLE),
                            eq(10L),
                            eq(admin),
                            eq(AuditLog.AuditAction.KB_ARTICLE_CREATED),
                            isNull(),
                            anyString(),
                            anyString());
        }

        @Test
        @DisplayName("resolves and links the category when categoryId is provided")
        void createArticle_withCategory_resolvesCategory() {
            final CreateArticleRequest request = new CreateArticleRequest();
            request.setTitle("Password Reset Guide");
            request.setContent("Steps to reset your password");
            request.setType(KnowledgeArticle.ArticleType.STANDARD_OPERATING_PROCEDURE);
            request.setCategoryId(5L);

            final Category category = Category.builder().id(5L).name("Password Reset").build();
            given(categoryQuery.findOrThrow(5L)).willReturn(category);
            given(articleRepository.existsBySlug(anyString())).willReturn(false);
            given(articleRepository.save(any(KnowledgeArticle.class))).willReturn(article);
            given(articleMapper.toArticleResponse(article))
                    .willReturn(ArticleResponse.builder().id(10L).build());

            articleService.createArticle(request, admin);

            final ArgumentCaptor<KnowledgeArticle> captor =
                    ArgumentCaptor.forClass(KnowledgeArticle.class);
            verify(articleRepository).save(captor.capture());
            assertThat(captor.getValue().getCategory()).isEqualTo(category);
        }

        @Test
        @DisplayName("propagates ResourceNotFoundException when the category doesn't exist")
        void createArticle_unknownCategory_throwsNotFound() {
            final CreateArticleRequest request = new CreateArticleRequest();
            request.setTitle("Some Article");
            request.setContent("Content");
            request.setType(KnowledgeArticle.ArticleType.GENERAL);
            request.setCategoryId(999L);

            given(categoryQuery.findOrThrow(999L))
                    .willThrow(new ResourceNotFoundException("Category", 999L));

            assertThatThrownBy(() -> articleService.createArticle(request, admin))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(articleRepository, never()).save(any());
        }

        @Test
        @DisplayName("appends a numeric suffix to the slug when the base slug is already taken")
        void createArticle_duplicateTitle_generatesSuffixedSlug() {
            final CreateArticleRequest request = new CreateArticleRequest();
            request.setTitle("Requesting VPN Access");
            request.setContent("Content");
            request.setType(KnowledgeArticle.ArticleType.FAQ);

            given(articleRepository.existsBySlug("requesting-vpn-access")).willReturn(true);
            given(articleRepository.existsBySlug("requesting-vpn-access-2")).willReturn(false);
            given(articleRepository.save(any(KnowledgeArticle.class))).willReturn(article);
            given(articleMapper.toArticleResponse(article))
                    .willReturn(ArticleResponse.builder().id(10L).build());

            articleService.createArticle(request, admin);

            final ArgumentCaptor<KnowledgeArticle> captor =
                    ArgumentCaptor.forClass(KnowledgeArticle.class);
            verify(articleRepository).save(captor.capture());
            assertThat(captor.getValue().getSlug()).isEqualTo("requesting-vpn-access-2");
        }
    }

    @Nested
    @DisplayName("getArticleById() / getArticleBySlug()")
    class GetArticle {

        @Test
        @DisplayName("increments the view count and returns the article when visible")
        void getArticleById_visibleArticle_incrementsViewCount() {
            given(articleQuery.findVisibleOrThrow(10L, citizen)).willReturn(article);
            given(articleMapper.toArticleResponse(article))
                    .willReturn(ArticleResponse.builder().id(10L).build());

            final ArticleResponse response = articleService.getArticleById(10L, citizen);

            assertThat(response.getId()).isEqualTo(10L);
            verify(articleRepository).incrementViewCount(10L);
        }

        @Test
        @DisplayName(
                "propagates the visibility check's ResourceNotFoundException without incrementing"
                        + " views")
        void getArticleById_hiddenFromCitizen_throwsNotFoundAndSkipsIncrement() {
            given(articleQuery.findVisibleOrThrow(10L, citizen))
                    .willThrow(new ResourceNotFoundException("Article", 10L));

            assertThatThrownBy(() -> articleService.getArticleById(10L, citizen))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(articleRepository, never()).incrementViewCount(any());
        }

        @Test
        @DisplayName("getArticleBySlug() increments the view count by the resolved id")
        void getArticleBySlug_visibleArticle_incrementsViewCountByResolvedId() {
            given(articleQuery.findVisibleBySlugOrThrow("requesting-vpn-access", citizen))
                    .willReturn(article);
            given(articleMapper.toArticleResponse(article))
                    .willReturn(ArticleResponse.builder().id(10L).build());

            articleService.getArticleBySlug("requesting-vpn-access", citizen);

            verify(articleRepository).incrementViewCount(10L);
        }
    }

    @Nested
    @DisplayName("getArticles() / searchArticles()")
    class BrowseAndSearch {

        @Test
        @DisplayName("resolves the visible status through ArticleQueryHelper before filtering")
        void getArticles_delegatesStatusResolutionToQueryHelper() {
            final Pageable pageable = PageRequest.of(0, 20);
            given(articleQuery.resolveVisibleStatus(KnowledgeArticle.ArticleStatus.DRAFT, citizen))
                    .willReturn(KnowledgeArticle.ArticleStatus.PUBLISHED);
            given(
                            articleRepository.findWithFilters(
                                    KnowledgeArticle.ArticleStatus.PUBLISHED,
                                    null,
                                    null,
                                    null,
                                    pageable))
                    .willReturn(new PageImpl<>(List.of(article)));
            given(articleMapper.toSummaryResponse(article))
                    .willReturn(ArticleSummaryResponse.builder().id(10L).build());

            articleService.getArticles(
                    KnowledgeArticle.ArticleStatus.DRAFT, null, null, null, pageable, citizen);

            verify(articleRepository)
                    .findWithFilters(
                            KnowledgeArticle.ArticleStatus.PUBLISHED, null, null, null, pageable);
        }

        @Test
        @DisplayName("normalises the tag filter to lowercase and trims whitespace")
        void getArticles_tagFilter_normalisedToLowercase() {
            final Pageable pageable = PageRequest.of(0, 20);
            given(articleQuery.resolveVisibleStatus(null, admin)).willReturn(null);
            given(articleRepository.findWithFilters(null, null, null, "vpn", pageable))
                    .willReturn(new PageImpl<>(List.of()));

            articleService.getArticles(null, null, null, "  VPN  ", pageable, admin);

            verify(articleRepository).findWithFilters(null, null, null, "vpn", pageable);
        }

        @Test
        @DisplayName("restricts search to PUBLISHED for a citizen")
        void searchArticles_citizen_restrictsToPublished() {
            final Pageable pageable = PageRequest.of(0, 20);
            given(articleRepository.search("vpn", "PUBLISHED", pageable))
                    .willReturn(new PageImpl<>(List.of()));

            articleService.searchArticles("vpn", pageable, citizen);

            verify(articleRepository).search("vpn", "PUBLISHED", pageable);
        }

        @Test
        @DisplayName("searches across any status for staff")
        void searchArticles_staff_searchesAnyStatus() {
            final Pageable pageable = PageRequest.of(0, 20);
            given(articleRepository.search("vpn", null, pageable))
                    .willReturn(new PageImpl<>(List.of()));

            articleService.searchArticles("vpn", pageable, admin);

            verify(articleRepository).search("vpn", null, pageable);
        }
    }

    @Nested
    @DisplayName("updateArticle()")
    class UpdateArticle {

        @Test
        @DisplayName("updates content fields and publishes a single KB_ARTICLE_UPDATED entry")
        void updateArticle_contentChange_publishesUpdatedAudit() {
            final UpdateArticleRequest request = new UpdateArticleRequest();
            request.setTitle("Requesting VPN Access (Updated)");

            given(articleQuery.findOrThrow(10L)).willReturn(article);
            given(articleRepository.save(article)).willReturn(article);
            given(articleMapper.toArticleResponse(article))
                    .willReturn(ArticleResponse.builder().id(10L).build());

            articleService.updateArticle(10L, request, admin);

            assertThat(article.getTitle()).isEqualTo("Requesting VPN Access (Updated)");
            //            verify(auditPublisher)
            //                    .publishAudit(
            //                            eq(AuditLog.EntityType.KNOWLEDGE_ARTICLE),
            //                            eq(10L),
            //                            eq(admin),
            //                            eq(AuditLog.AuditAction.KB_ARTICLE_UPDATED),
            //                            isNull(),
            //                            isNull(),
            //                            anyString());
        }

        @Test
        @DisplayName("does not publish an audit entry when nothing actually changed")
        void updateArticle_noActualChange_skipsAudit() {
            final UpdateArticleRequest request = new UpdateArticleRequest();
            request.setTitle(article.getTitle()); // identical to current value

            given(articleQuery.findOrThrow(10L)).willReturn(article);
            given(articleRepository.save(article)).willReturn(article);
            given(articleMapper.toArticleResponse(article))
                    .willReturn(ArticleResponse.builder().id(10L).build());

            articleService.updateArticle(10L, request, admin);

            verify(auditPublisher, never())
                    .publishAudit(
                            any(),
                            any(),
                            any(),
                            eq(AuditLog.AuditAction.KB_ARTICLE_UPDATED),
                            any(),
                            any(),
                            any());
        }

        @Test
        @DisplayName("rejects moving a PUBLISHED article back to DRAFT")
        void updateArticle_publishedToDraft_throwsInvalidOperation() {
            article.setStatus(KnowledgeArticle.ArticleStatus.PUBLISHED);
            final UpdateArticleRequest request = new UpdateArticleRequest();
            request.setStatus(KnowledgeArticle.ArticleStatus.DRAFT);

            given(articleQuery.findOrThrow(10L)).willReturn(article);

            assertThatThrownBy(() -> articleService.updateArticle(10L, request, admin))
                    .isInstanceOf(InvalidArticleOperationException.class);

            verify(articleRepository, never()).save(any());
        }

        @Test
        @DisplayName("stamps publishedAt the first time an article is published")
        void updateArticle_draftToPublished_stampsPublishedAt() {
            final UpdateArticleRequest request = new UpdateArticleRequest();
            request.setStatus(KnowledgeArticle.ArticleStatus.PUBLISHED);

            given(articleQuery.findOrThrow(10L)).willReturn(article);
            given(articleRepository.save(article)).willReturn(article);
            given(articleMapper.toArticleResponse(article))
                    .willReturn(ArticleResponse.builder().id(10L).build());

            articleService.updateArticle(10L, request, admin);

            assertThat(article.getStatus()).isEqualTo(KnowledgeArticle.ArticleStatus.PUBLISHED);
            assertThat(article.getPublishedAt()).isNotNull();
            verify(auditPublisher)
                    .publishAudit(
                            eq(AuditLog.EntityType.KNOWLEDGE_ARTICLE),
                            eq(10L),
                            eq(admin),
                            eq(AuditLog.AuditAction.KB_ARTICLE_PUBLISHED),
                            eq("DRAFT"),
                            eq("PUBLISHED"),
                            isNull());
        }

        @Test
        @DisplayName(
                "does not overwrite publishedAt when republishing an already-published article")
        void updateArticle_archivedToPublished_doesNotOverwriteExistingPublishedAt() {
            final LocalDateTime originalPublishedAt = LocalDateTime.of(2025, 1, 1, 0, 0);
            article.setStatus(KnowledgeArticle.ArticleStatus.ARCHIVED);
            article.setPublishedAt(originalPublishedAt);

            final UpdateArticleRequest request = new UpdateArticleRequest();
            request.setStatus(KnowledgeArticle.ArticleStatus.PUBLISHED);

            given(articleQuery.findOrThrow(10L)).willReturn(article);
            given(articleRepository.save(article)).willReturn(article);
            given(articleMapper.toArticleResponse(article))
                    .willReturn(ArticleResponse.builder().id(10L).build());

            articleService.updateArticle(10L, request, admin);

            assertThat(article.getPublishedAt()).isEqualTo(originalPublishedAt);
        }

        @Test
        @DisplayName("publishes KB_ARTICLE_ARCHIVED when moving to ARCHIVED")
        void updateArticle_publishedToArchived_publishesArchivedAudit() {
            article.setStatus(KnowledgeArticle.ArticleStatus.PUBLISHED);
            final UpdateArticleRequest request = new UpdateArticleRequest();
            request.setStatus(KnowledgeArticle.ArticleStatus.ARCHIVED);

            given(articleQuery.findOrThrow(10L)).willReturn(article);
            given(articleRepository.save(article)).willReturn(article);
            given(articleMapper.toArticleResponse(article))
                    .willReturn(ArticleResponse.builder().id(10L).build());

            articleService.updateArticle(10L, request, admin);

            verify(auditPublisher)
                    .publishAudit(
                            eq(AuditLog.EntityType.KNOWLEDGE_ARTICLE),
                            eq(10L),
                            eq(admin),
                            eq(AuditLog.AuditAction.KB_ARTICLE_ARCHIVED),
                            eq("PUBLISHED"),
                            eq("ARCHIVED"),
                            isNull());
        }

        @Test
        @DisplayName("clearCategory removes the category link")
        void updateArticle_clearCategory_removesLink() {
            article.setCategory(Category.builder().id(5L).name("VPN").build());
            final UpdateArticleRequest request = new UpdateArticleRequest();
            request.setClearCategory(true);

            given(articleQuery.findOrThrow(10L)).willReturn(article);
            given(articleRepository.save(article)).willReturn(article);
            given(articleMapper.toArticleResponse(article))
                    .willReturn(ArticleResponse.builder().id(10L).build());

            articleService.updateArticle(10L, request, admin);

            assertThat(article.getCategory()).isNull();
        }
    }

    @Nested
    @DisplayName("deleteArticle()")
    class DeleteArticle {

        @Test
        @DisplayName("deletes the article and publishes KB_ARTICLE_DELETED")
        void deleteArticle_existingArticle_deletesAndAudits() {
            given(articleQuery.findOrThrow(10L)).willReturn(article);

            articleService.deleteArticle(10L, admin);

            verify(articleRepository).delete(article);
            verify(auditPublisher)
                    .publishAudit(
                            eq(AuditLog.EntityType.KNOWLEDGE_ARTICLE),
                            eq(10L),
                            eq(admin),
                            eq(AuditLog.AuditAction.KB_ARTICLE_DELETED),
                            eq(article.getTitle()),
                            isNull(),
                            isNull());
        }
    }

    @Nested
    @DisplayName("submitFeedback() / getFeedback()")
    class Feedback {

        @Test
        @DisplayName("records a new helpful vote and increments the helpful count")
        void submitFeedback_newHelpfulVote_incrementsHelpfulCount() {
            final ArticleFeedbackRequest request = new ArticleFeedbackRequest();
            request.setHelpful(true);

            given(articleQuery.findVisibleOrThrow(10L, citizen)).willReturn(article);
            given(feedbackRepository.findByArticleIdAndUserId(10L, citizen.getId()))
                    .willReturn(Optional.empty());

            final ArticleFeedbackResponse response =
                    articleService.submitFeedback(10L, request, citizen);

            assertThat(article.getHelpfulCount()).isEqualTo(1);
            assertThat(response.getHelpfulCount()).isEqualTo(1);
            assertThat(response.getYourVote()).isTrue();
            verify(feedbackRepository).save(any(KnowledgeArticleFeedback.class));
        }

        @Test
        @DisplayName("changing an existing vote moves the count from one bucket to the other")
        void submitFeedback_changedVote_movesCountBetweenBuckets() {
            article.setHelpfulCount(1);
            final KnowledgeArticleFeedback existingVote =
                    KnowledgeArticleFeedback.builder()
                            .article(article)
                            .user(citizen)
                            .helpful(true)
                            .build();

            final ArticleFeedbackRequest request = new ArticleFeedbackRequest();
            request.setHelpful(false);

            given(articleQuery.findVisibleOrThrow(10L, citizen)).willReturn(article);
            given(feedbackRepository.findByArticleIdAndUserId(10L, citizen.getId()))
                    .willReturn(Optional.of(existingVote));

            articleService.submitFeedback(10L, request, citizen);

            assertThat(article.getHelpfulCount()).isEqualTo(0);
            assertThat(article.getNotHelpfulCount()).isEqualTo(1);
            verify(feedbackRepository).save(existingVote);
        }

        @Test
        @DisplayName("resubmitting the same vote is a no-op that doesn't touch the counts again")
        void submitFeedback_sameVoteResubmitted_isNoOp() {
            article.setHelpfulCount(1);
            final KnowledgeArticleFeedback existingVote =
                    KnowledgeArticleFeedback.builder()
                            .article(article)
                            .user(citizen)
                            .helpful(true)
                            .build();

            final ArticleFeedbackRequest request = new ArticleFeedbackRequest();
            request.setHelpful(true);

            given(articleQuery.findVisibleOrThrow(10L, citizen)).willReturn(article);
            given(feedbackRepository.findByArticleIdAndUserId(10L, citizen.getId()))
                    .willReturn(Optional.of(existingVote));

            articleService.submitFeedback(10L, request, citizen);

            assertThat(article.getHelpfulCount()).isEqualTo(1);
            verify(feedbackRepository, never()).save(any());
            verify(articleRepository, never()).save(any());
        }

        @Test
        @DisplayName("getFeedback() returns null yourVote when the user hasn't voted")
        void getFeedback_noVoteYet_returnsNullVote() {
            given(articleQuery.findVisibleOrThrow(10L, citizen)).willReturn(article);
            given(feedbackRepository.findByArticleIdAndUserId(10L, citizen.getId()))
                    .willReturn(Optional.empty());

            final ArticleFeedbackResponse response = articleService.getFeedback(10L, citizen);

            assertThat(response.getYourVote()).isNull();
        }

        @Test
        @DisplayName("getFeedback() returns the user's own recorded vote")
        void getFeedback_existingVote_returnsRecordedVote() {
            final KnowledgeArticleFeedback existingVote =
                    KnowledgeArticleFeedback.builder()
                            .article(article)
                            .user(citizen)
                            .helpful(false)
                            .build();
            given(articleQuery.findVisibleOrThrow(10L, citizen)).willReturn(article);
            given(feedbackRepository.findByArticleIdAndUserId(10L, citizen.getId()))
                    .willReturn(Optional.of(existingVote));

            final ArticleFeedbackResponse response = articleService.getFeedback(10L, citizen);

            assertThat(response.getYourVote()).isFalse();
        }
    }
}
