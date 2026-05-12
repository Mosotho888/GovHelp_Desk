package za.gov.helpdesk.emailnotification.model;

import za.gov.helpdesk.ticket.dto.response.TicketResponse;
import za.gov.helpdesk.ticket.model.Ticket;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity(name = "email_notifications")
public class EmailNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @Column(name = "recipient")
    private String recipient;
    @Column(name = "subject")
    private String subject;
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    @Column(name = "status")
    private EmailStatus status;

    public EmailNotification() {
    }

    public EmailNotification(Long id, Ticket ticket, String recipient, String subject, String body, LocalDateTime sentAt, EmailStatus status) {
        this.id = id;
        this.ticket = ticket;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.sentAt = sentAt;
        this.status = status;
    }

    public enum EmailStatus {
        PENDING,
        SENT,
        FAILED
    }
}
