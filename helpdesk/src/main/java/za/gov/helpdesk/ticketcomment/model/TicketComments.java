package za.gov.helpdesk.ticketcomment.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import za.gov.helpdesk.ticket.model.Ticket;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "ticket_comments")
public class TicketComments {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticket_id")
    @JsonIgnore
    private Ticket ticket;

    @ManyToOne
    @JoinColumn(name = "commenter_id")
    private za.gov.helpdesk.users.model.User commenter;
    @NotNull(message = "comment is required")
    private String comment;
    private LocalDateTime created_at;

    public TicketComments() {
    }

    public TicketComments(Long id, Ticket ticket, za.gov.helpdesk.users.model.User commenter, String comment) {
        this.id = id;
        this.ticket = ticket;
        this.commenter = commenter;
        this.comment = comment;
    }

}
