package za.gov.helpdesk.asset.model;

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
 * Links a {@link Ticket} to an {@link Asset} it concerns - a many-to-many association modelled as
 * its own entity (rather than a raw {@code @ManyToMany}) so the link itself carries an audit trail
 * (who linked it, when), matching the rest of the codebase's preference for explicit join records
 * over managed collections. A ticket may reference more than one asset (e.g. a network outage
 * affecting several devices), and an asset accumulates one row per ticket raised against it over
 * its lifetime - that history is exactly what {@code GET /v1/assets/{id}/tickets} reads back.
 */
@Setter
@Getter
@Entity
@Table(
        name = "TICKET_ASSETS",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ticket_id", "asset_id"}))
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "linked_by_id", nullable = false)
    private Long linkedById;

    /** Denormalised at write time, same rationale as {@code audit_log.actor_name}. */
    @Column(name = "linked_by_name", nullable = false, length = 100)
    private String linkedByName;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private LocalDateTime linkedAt;

    @PrePersist
    protected void onCreate() {
        this.linkedAt = LocalDateTime.now();
    }
}
