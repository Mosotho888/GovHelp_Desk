package za.gov.helpdesk.knowledgebase.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.users.model.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A piece of self-service documentation: a troubleshooting guide, FAQ, or standard operating
 * procedure. Articles are searchable via a generated {@code search_vector} column (see {@code
 * V10__create_knowledge_base.sql}), which is deliberately not mapped here since it is only ever
 * queried through the native full-text search query in {@link
 * za.gov.helpdesk.knowledgebase.repository.KnowledgeArticleRepository}, never read or written via
 * the entity itself.
 */
@SuppressWarnings({"checkstyle:ClassFanOutComplexity", "PMD.TooManyFields"})
@Setter
@Getter
@Entity
@Table(name = "KNOWLEDGE_ARTICLES")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(length = 500)
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ArticleType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ArticleStatus status = ArticleStatus.DRAFT;

    /**
     * Optional link to the ticket category this article helps with, for self-service suggestions.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "knowledge_article_tags",
            joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "tag", length = 50)
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "helpful_count", nullable = false)
    private int helpfulCount;

    @Column(name = "not_helpful_count", nullable = false)
    private int notHelpfulCount;

    /**
     * Number of times this article has ever been linked to a ticket - a running total that isn't
     * decremented on unlink, so it reflects how often the article has helped resolve a ticket over
     * its lifetime rather than how many tickets currently reference it.
     */
    @Column(name = "usage_count", nullable = false)
    private int usageCount;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum ArticleType {
        TROUBLESHOOTING_GUIDE,
        FAQ,
        STANDARD_OPERATING_PROCEDURE,
        GENERAL
    }

    public enum ArticleStatus {
        DRAFT,
        PUBLISHED,
        ARCHIVED
    }
}
