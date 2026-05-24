package za.gov.helpdesk.auditlog.model;

import jakarta.persistence.*;
import lombok.*;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.users.model.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUDIT_LOG", indexes = {
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id, created_at DESC"),
        @Index(name = "idx_audit_actor",  columnList = "actor_id, created_at DESC"),
        @Index(name = "idx_audit_action", columnList = "action, created_at DESC"),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 30)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "actor_name", nullable = false,  length = 100)
    private String actorName;

    @Column(name = "actor_role", nullable = false, length = 20)
    private String actorRole;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;

    @Column(name = "old_value", length = 50)
    private String oldValue;

    @Column(name = "new_value", length = 50)
    private String newValue;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum EntityType {
        TICKET,
        USER,
        AGENT,
        COMMENT,
        ATTACHMENT,
        AUTH
    }

    public enum AuditAction {
        TICKET_CREATED, STATUS_CHANGED, ASSIGNED_TO_AGENT, ESCALATED, PRIORITY_CHANGED, TICKET_DELETED,
        TICKET_CLOSED, USER_CREATED, USER_UPDATED, USER_DEACTIVATED, USER_REACTIVATED, ROLE_CHANGED, PASSWORD_RESET,
        AGENT_REGISTERED, AVAILABILITY_CHANGED, DEPARTMENT_CHANGED, COMMENT_ADDED, COMMENT_EDITED,
        COMMENT_DELETED, INTERNAL_NOTE_ADDED, ATTACHMENT_UPLOADED, ATTACHMENT_DELETED, ATTACHMENT_DOWNLOADED,
        LOGIN_SUCCESS, LOGIN_FAILED, ACCOUNT_LOCKED, TOKEN_REFRESHED, FORCED_LOGOUT
    }

}
