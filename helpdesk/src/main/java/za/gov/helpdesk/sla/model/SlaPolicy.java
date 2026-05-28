package za.gov.helpdesk.sla.model;

import jakarta.persistence.*;
import lombok.*;
import za.gov.helpdesk.ticket.model.Ticket;

@Entity
@Table(name = "sla_policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private Ticket.Priority priority;

    // How many business minutes until first response is due
    @Column(name = "response_minutes", nullable = false)
    private int responseMinutes;

    // How many business minutes until ticket must be resolved
    @Column(name = "resolution_minutes", nullable = false)
    private int resolutionMinutes;

    // Warning sent this many business minutes before breach
    @Column(name = "warning_threshold_minutes", nullable = false)
    @Builder.Default
    private int warningThresholdMinutes = 30;
}
