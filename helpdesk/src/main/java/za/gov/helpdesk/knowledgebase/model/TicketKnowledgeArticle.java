package za.gov.helpdesk.knowledgebase.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import za.gov.helpdesk.ticket.model.Ticket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Links a {@link Ticket} to a {@link KnowledgeArticle} that helped resolve it - the same explicit
 * join-entity pattern used for {@code TicketAsset}, so the link carries who created it and when.
 * This is the data behind two related, valuable views: "what article resolved this ticket" from the
 * ticket's side, and "how often has this article actually helped" from the article's side (see
 * {@link KnowledgeArticle#getUsageCount()}), which in turn surfaces which self-service content is
 * worth investing in versus which categories of ticket still lack a documented answer.
 */
@Setter
@Getter
@Entity
@Table(
        name = "TICKET_KNOWLEDGE_ARTICLES",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ticket_id", "article_id"}))
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketKnowledgeArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private KnowledgeArticle article;

    @Column(name = "linked_by_id", nullable = false)
    private Long linkedById;

    @Column(name = "linked_by_name", nullable = false, length = 100)
    private String linkedByName;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private LocalDateTime linkedAt;

    @PrePersist
    protected void onCreate() {
        this.linkedAt = LocalDateTime.now();
    }
}
