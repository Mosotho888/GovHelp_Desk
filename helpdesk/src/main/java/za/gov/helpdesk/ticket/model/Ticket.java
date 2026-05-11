package za.gov.helpdesk.ticket.model;

import lombok.*;
import za.gov.helpdesk.agent.model.Agent;
import jakarta.persistence.*;
import za.gov.helpdesk.users.model.User;

import java.time.LocalDateTime;


@Setter
@Getter
@Entity
@Table(name = "TICKETS")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(length = 100)
    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private Agent assignee;

    @Column(nullable = false)
    @Builder.Default
    private boolean escalated = false;

    @Column(name = "created_at",  nullable = false, updatable = false)
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

    public boolean canTransitionTo(Status newStatus) {
        return switch (this.status) {
            case OPEN, ESCALATED -> newStatus == Status.IN_PROGRESS;
            case IN_PROGRESS -> newStatus == Status.RESOLVED || newStatus == Status.ESCALATED;
            case RESOLVED -> newStatus == Status.CLOSED || newStatus == Status.OPEN;
            case CLOSED -> false;
        };
    }

    public enum Status {
        OPEN,
        IN_PROGRESS,
        ESCALATED,
        RESOLVED,
        CLOSED
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }
}
