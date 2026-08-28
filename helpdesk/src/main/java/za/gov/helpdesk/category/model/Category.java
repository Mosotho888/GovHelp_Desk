package za.gov.helpdesk.category.model;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Hierarchical ticket classification node. Categories form a shallow tree (Category &gt;
 * Subcategory &gt; Type, capped at {@link #MAX_LEVEL}) used to organise tickets, drive automated
 * routing defaults, and power category-based reporting and filtering.
 */
@Setter
@Getter
@Entity
@Table(name = "TICKET_CATEGORIES")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    /** Maximum allowed depth: 0 = top level, 1 = subcategory, 2 = type. */
    public static final int MAX_LEVEL = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(nullable = false)
    private short level;

    /**
     * The team/department a ticket should route to by default when it lands in this category, if no
     * more specific override applies. Kept as a plain label (mirrors {@code Agent.department})
     * rather than a hard FK, since routing groups are informal at this stage of the system.
     */
    @Column(name = "default_department", length = 100)
    private String defaultDepartment;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

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

    public boolean isRoot() {
        return parent == null;
    }
}
