package za.gov.helpdesk.agent.model;

import jakarta.persistence.*;
import lombok.*;
import za.gov.helpdesk.users.model.User;

@Entity
@Table(name = "AGENTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 100)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Availability availability = Availability.OFFLINE;

    public enum Availability {
        ONLINE,
        BUSY,
        AWAY,
        OFFLINE
    }
}
