package za.gov.helpdesk.sla.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.gov.helpdesk.ticket.model.Ticket;

@Entity
@Table(name = "ticket_sla")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketSla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private Ticket ticket;

    @Column(name = "response_due_at", nullable = false)
    private LocalDateTime responseDueAt;

    @Column(name = "resolution_due_at", nullable = false)
    private LocalDateTime resolutionDueAt;

    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "response_breached", nullable = false)
    @Builder.Default
    private boolean responseBreached = false;

    @Column(name = "resolution_breached", nullable = false)
    @Builder.Default
    private boolean resolutionBreached = false;

    @Column(name = "response_warning_sent", nullable = false)
    @Builder.Default
    private boolean responseWarningSent = false;

    @Column(name = "resolution_warning_sent", nullable = false)
    @Builder.Default
    private boolean resolutionWarningSent = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isResponseBreached() {
        return responseBreached || (firstResponseAt == null && LocalDateTime.now().isAfter(responseDueAt));
    }

    public boolean isResolutionBreached() {
        return resolutionBreached || (resolvedAt == null && LocalDateTime.now().isAfter(resolutionDueAt));
    }
}
