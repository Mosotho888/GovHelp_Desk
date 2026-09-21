package za.gov.helpdesk.unit.knowledgebase;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
import za.gov.helpdesk.knowledgebase.service.impl.TicketArticleLinkServiceImpl;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.service.TicketQueryHelper;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketArticleLinkServiceImpl unit tests")
class TicketArticleLinkServiceImplTest {

    @Mock private TicketKnowledgeArticleRepository ticketArticleRepository;
    @Mock private KnowledgeArticleRepository articleRepository;
    @Mock private TicketQueryHelper ticketQuery;
    @Mock private ArticleQueryHelper articleQuery;
    @Mock private ArticleMapper articleMapper;
    @Mock private AuditEventPublisher auditPublisher;

    private TicketArticleLinkServiceImpl linkService;

    private User agent;
    private Ticket ticket;
    private KnowledgeArticle article;

    @BeforeEach
    void setUp() {
        linkService =
                new TicketArticleLinkServiceImpl(
                        ticketArticleRepository,
                        articleRepository,
                        ticketQuery,
                        articleQuery,
                        articleMapper,
                        auditPublisher);

        agent = User.builder().id(1L).name("Agent").role(Role.AGENT).build();
        ticket = Ticket.builder().id(50L).subject("VPN not connecting").build();
        article =
                KnowledgeArticle.builder()
                        .id(10L)
                        .title("Requesting VPN Access")
                        .slug("requesting-vpn-access")
                        .usageCount(0)
                        .build();
    }

    @Nested
    @DisplayName("linkArticleToTicket()")
    class LinkArticleToTicket {

        @Test
        @DisplayName("creates the link, increments usageCount, and audits both entities")
        void linkArticleToTicket_newLink_incrementsUsageAndAuditsBothEntities() {
            given(ticketQuery.findOrThrow(50L, agent)).willReturn(ticket);
            given(articleQuery.findOrThrow(10L)).willReturn(article);
            given(ticketArticleRepository.existsByTicketIdAndArticleId(50L, 10L)).willReturn(false);
            given(ticketArticleRepository.save(any(TicketKnowledgeArticle.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            final TicketArticleLinkResponse response =
                    linkService.linkArticleToTicket(50L, 10L, agent);

            assertThat(response.getTicketId()).isEqualTo(50L);
            assertThat(response.getArticleId()).isEqualTo(10L);
            assertThat(response.getArticleTitle()).isEqualTo("Requesting VPN Access");

            assertThat(article.getUsageCount()).isEqualTo(1);
            verify(articleRepository).save(article);

            verify(auditPublisher)
                    .publishAudit(
                            eq(AuditLog.EntityType.TICKET),
                            eq(50L),
                            eq(agent),
                            eq(AuditLog.AuditAction.KB_ARTICLE_LINKED_TO_TICKET),
                            isNull(),
                            anyString(),
                            anyString());
            verify(auditPublisher)
                    .publishAudit(
                            eq(AuditLog.EntityType.KNOWLEDGE_ARTICLE),
                            eq(10L),
                            eq(agent),
                            eq(AuditLog.AuditAction.KB_ARTICLE_LINKED_TO_TICKET),
                            isNull(),
                            anyString(),
                            anyString());
        }

        @Test
        @DisplayName("rejects linking the same article to the same ticket twice")
        void linkArticleToTicket_alreadyLinked_throwsDuplicateResource() {
            given(ticketQuery.findOrThrow(50L, agent)).willReturn(ticket);
            given(articleQuery.findOrThrow(10L)).willReturn(article);
            given(ticketArticleRepository.existsByTicketIdAndArticleId(50L, 10L)).willReturn(true);

            assertThatThrownBy(() -> linkService.linkArticleToTicket(50L, 10L, agent))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(ticketArticleRepository, never()).save(any());
            verify(articleRepository, never()).save(any());
        }

        @Test
        @DisplayName(
                "propagates ResourceNotFoundException when the ticket doesn't exist or isn't"
                        + " accessible")
        void linkArticleToTicket_ticketNotAccessible_propagatesNotFound() {
            given(ticketQuery.findOrThrow(50L, agent))
                    .willThrow(new ResourceNotFoundException("Ticket", 50L));

            assertThatThrownBy(() -> linkService.linkArticleToTicket(50L, 10L, agent))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(articleQuery, never()).findOrThrow(any());
        }
    }

    @Nested
    @DisplayName("unlinkArticleFromTicket()")
    class UnlinkArticleFromTicket {

        @Test
        @DisplayName("deletes the link and audits both entities, without touching usageCount")
        void unlinkArticleFromTicket_existingLink_deletesAndAuditsWithoutTouchingUsageCount() {
            final TicketKnowledgeArticle link =
                    TicketKnowledgeArticle.builder().ticket(ticket).article(article).build();
            given(ticketQuery.findOrThrow(50L, agent)).willReturn(ticket);
            given(ticketArticleRepository.findByTicketIdAndArticleId(50L, 10L))
                    .willReturn(Optional.of(link));

            linkService.unlinkArticleFromTicket(50L, 10L, agent);

            verify(ticketArticleRepository).delete(link);
            verify(articleRepository, never()).save(any());

            verify(auditPublisher, times(2))
                    .publishAudit(
                            any(),
                            any(),
                            eq(agent),
                            eq(AuditLog.AuditAction.KB_ARTICLE_UNLINKED_FROM_TICKET),
                            any(),
                            any(),
                            anyString());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when the article isn't linked to the ticket")
        void unlinkArticleFromTicket_notLinked_throwsNotFound() {
            given(ticketQuery.findOrThrow(50L, agent)).willReturn(ticket);
            given(ticketArticleRepository.findByTicketIdAndArticleId(50L, 10L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> linkService.unlinkArticleFromTicket(50L, 10L, agent))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(ticketArticleRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("getArticlesForTicket()")
    class GetArticlesForTicket {

        @Test
        @DisplayName("returns articles linked to the ticket, most recently linked first")
        void getArticlesForTicket_returnsLinkedArticles() {
            final TicketKnowledgeArticle link =
                    TicketKnowledgeArticle.builder().ticket(ticket).article(article).build();
            given(ticketQuery.findOrThrow(50L, agent)).willReturn(ticket);
            given(ticketArticleRepository.findByTicketIdOrderByLinkedAtDesc(50L))
                    .willReturn(List.of(link));
            given(articleMapper.toSummaryResponse(article))
                    .willReturn(ArticleSummaryResponse.builder().id(10L).build());

            final List<ArticleSummaryResponse> result =
                    linkService.getArticlesForTicket(50L, agent);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("still enforces ticket access even when there are no linked articles")
        void getArticlesForTicket_noAccess_propagatesNotFound() {
            given(ticketQuery.findOrThrow(50L, agent))
                    .willThrow(new ResourceNotFoundException("Ticket", 50L));

            assertThatThrownBy(() -> linkService.getArticlesForTicket(50L, agent))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getTicketHistoryForArticle()")
    class GetTicketHistoryForArticle {

        @Test
        @DisplayName(
                "maps linked tickets into history entries, validating the article exists first")
        void getTicketHistoryForArticle_returnsHistory() {
            final Pageable pageable = PageRequest.of(0, 20);
            final TicketKnowledgeArticle link =
                    TicketKnowledgeArticle.builder().ticket(ticket).article(article).build();

            given(articleQuery.findOrThrow(10L)).willReturn(article);
            given(ticketArticleRepository.findByArticleIdOrderByLinkedAtDesc(10L, pageable))
                    .willReturn(new PageImpl<>(List.of(link)));

            final Page<ArticleTicketHistoryResponse> result =
                    linkService.getTicketHistoryForArticle(10L, pageable);

            assertThat(result.getContent()).hasSize(1);
            final ArticleTicketHistoryResponse entry = result.getContent().get(0);
            assertThat(entry.getTicketId()).isEqualTo(50L);
            assertThat(entry.getSubject()).isEqualTo("VPN not connecting");
        }

        @Test
        @DisplayName("propagates ResourceNotFoundException for an unknown article")
        void getTicketHistoryForArticle_unknownArticle_throwsNotFound() {
            final Pageable pageable = PageRequest.of(0, 20);
            given(articleQuery.findOrThrow(999L))
                    .willThrow(new ResourceNotFoundException("Article", 999L));

            assertThatThrownBy(() -> linkService.getTicketHistoryForArticle(999L, pageable))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
