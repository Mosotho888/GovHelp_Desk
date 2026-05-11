package za.gov.helpdesk.emailnotification.dto;

import za.gov.helpdesk.ticket.model.Ticket;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class EmailNotificationDTO implements Serializable {
    private String technicianEmail;
    private String normalUserEmail;
    private String technicianName;
    private Long ticketId;
    private String comment;
    private String status;
    private String priority;
    private String category;
    private String createdAt;
    private String dueAt;
    private String updatedAt;
    private String issueDescription;

    public EmailNotificationDTO(Ticket ticket, String comment) {
        this.technicianEmail = ticket.getAssignee().getUser().getEmail();
        this.normalUserEmail = ticket.getRequester().getEmail();
        this.technicianName = ticket.getAssignee().getUser().getName();
        this.ticketId = ticket.getId();
        this.comment = comment;
        this.status = ticket.getStatus().name();
        this.priority = ticket.getPriority().name();
        this.category = ticket.getCategory();
        this.createdAt = ticket.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.dueAt = ticket.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        this.updatedAt = checkUpdateAtDate(ticket.getUpdatedAt());
        this.issueDescription = ticket.getDescription();
    }

    public String checkUpdateAtDate(LocalDateTime updateAt) {
        if (isNotNull(updateAt)) {
            return updateAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        return "N/A";
    }

    private static boolean isNotNull(LocalDateTime updateAt) {
        return updateAt != null;
    }
}
