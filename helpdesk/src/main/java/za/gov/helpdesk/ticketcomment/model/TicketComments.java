package za.gov.helpdesk.ticketcomment.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import za.gov.helpdesk.employee.model.Employees;
import za.gov.helpdesk.ticket.model.Tickets;
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
    private Tickets tickets;

    @ManyToOne
    @JoinColumn(name = "commenter_id")
    private Employees commenter;
    @NotNull(message = "comment is required")
    private String comment;
    private LocalDateTime created_at;

    public TicketComments() {
    }

    public TicketComments(Long id, Tickets tickets, Employees commenter, String comment) {
        this.id = id;
        this.tickets = tickets;
        this.commenter = commenter;
        this.comment = comment;
    }

}
