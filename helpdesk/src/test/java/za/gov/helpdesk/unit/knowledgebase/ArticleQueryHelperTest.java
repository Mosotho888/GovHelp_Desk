package za.gov.helpdesk.unit.knowledgebase;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.knowledgebase.model.KnowledgeArticle;
import za.gov.helpdesk.knowledgebase.repository.KnowledgeArticleRepository;
import za.gov.helpdesk.knowledgebase.service.ArticleQueryHelper;
import za.gov.helpdesk.users.model.Role;
import za.gov.helpdesk.users.model.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleQueryHelper unit tests")
class ArticleQueryHelperTest {

    @Mock private KnowledgeArticleRepository articleRepository;

    @InjectMocks private ArticleQueryHelper articleQuery;

    private User citizen;
    private User agent;
    private KnowledgeArticle publishedArticle;
    private KnowledgeArticle draftArticle;

    @BeforeEach
    void setUp() {
        citizen = User.builder().id(1L).name("Citizen").role(Role.USER).build();
        agent = User.builder().id(2L).name("Agent").role(Role.AGENT).build();

        publishedArticle =
                KnowledgeArticle.builder()
                        .id(100L)
                        .title("Requesting VPN Access")
                        .slug("requesting-vpn-access")
                        .status(KnowledgeArticle.ArticleStatus.PUBLISHED)
                        .build();

        draftArticle =
                KnowledgeArticle.builder()
                        .id(200L)
                        .title("Draft Article")
                        .slug("draft-article")
                        .status(KnowledgeArticle.ArticleStatus.DRAFT)
                        .build();
    }

    @Test
    @DisplayName("findOrThrow() returns the article regardless of status")
    void findOrThrow_existingId_returnsArticle() {
        given(articleRepository.findById(200L)).willReturn(Optional.of(draftArticle));

        final KnowledgeArticle result = articleQuery.findOrThrow(200L);

        assertThat(result).isEqualTo(draftArticle);
    }

    @Test
    @DisplayName("findOrThrow() throws ResourceNotFoundException for unknown id")
    void findOrThrow_unknownId_throwsNotFound() {
        given(articleRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> articleQuery.findOrThrow(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("findVisibleOrThrow() returns a published article to a citizen")
    void findVisibleOrThrow_publishedArticleForCitizen_returnsArticle() {
        given(articleRepository.findById(100L)).willReturn(Optional.of(publishedArticle));

        final KnowledgeArticle result = articleQuery.findVisibleOrThrow(100L, citizen);

        assertThat(result).isEqualTo(publishedArticle);
    }

    @Test
    @DisplayName("findVisibleOrThrow() hides a draft article from a citizen behind a 404")
    void findVisibleOrThrow_draftArticleForCitizen_throwsNotFound() {
        given(articleRepository.findById(200L)).willReturn(Optional.of(draftArticle));

        assertThatThrownBy(() -> articleQuery.findVisibleOrThrow(200L, citizen))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("findVisibleOrThrow() lets an agent see a draft article")
    void findVisibleOrThrow_draftArticleForAgent_returnsArticle() {
        given(articleRepository.findById(200L)).willReturn(Optional.of(draftArticle));

        final KnowledgeArticle result = articleQuery.findVisibleOrThrow(200L, agent);

        assertThat(result).isEqualTo(draftArticle);
    }

    @Test
    @DisplayName("findVisibleBySlugOrThrow() hides an archived article from a citizen behind a 404")
    void findVisibleBySlugOrThrow_archivedArticleForCitizen_throwsNotFound() {
        draftArticle.setStatus(KnowledgeArticle.ArticleStatus.ARCHIVED);
        given(articleRepository.findBySlug("draft-article")).willReturn(Optional.of(draftArticle));

        assertThatThrownBy(() -> articleQuery.findVisibleBySlugOrThrow("draft-article", citizen))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("findVisibleBySlugOrThrow() throws ResourceNotFoundException for unknown slug")
    void findVisibleBySlugOrThrow_unknownSlug_throwsNotFound() {
        given(articleRepository.findBySlug("does-not-exist")).willReturn(Optional.empty());

        assertThatThrownBy(() -> articleQuery.findVisibleBySlugOrThrow("does-not-exist", agent))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName(
            "resolveVisibleStatus() forces PUBLISHED for a citizen regardless of requested status")
    void resolveVisibleStatus_citizenRequestingDraft_forcedToPublished() {
        final KnowledgeArticle.ArticleStatus resolved =
                articleQuery.resolveVisibleStatus(KnowledgeArticle.ArticleStatus.DRAFT, citizen);

        assertThat(resolved).isEqualTo(KnowledgeArticle.ArticleStatus.PUBLISHED);
    }

    @Test
    @DisplayName("resolveVisibleStatus() passes through the requested status for staff")
    void resolveVisibleStatus_staffRequestingDraft_passesThrough() {
        final KnowledgeArticle.ArticleStatus resolved =
                articleQuery.resolveVisibleStatus(KnowledgeArticle.ArticleStatus.DRAFT, agent);

        assertThat(resolved).isEqualTo(KnowledgeArticle.ArticleStatus.DRAFT);
    }

    @Test
    @DisplayName("resolveVisibleStatus() passes through a null (any-status) request for staff")
    void resolveVisibleStatus_staffRequestingNull_passesThroughNull() {
        final KnowledgeArticle.ArticleStatus resolved =
                articleQuery.resolveVisibleStatus(null, agent);

        assertThat(resolved).isNull();
    }
}
