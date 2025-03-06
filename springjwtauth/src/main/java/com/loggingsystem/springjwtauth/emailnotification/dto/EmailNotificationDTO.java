package com.loggingsystem.springjwtauth.emailnotification.dto;

import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class EmailNotificationDTO implements Serializable {
    private String technicianEmail;
    private String normalUserEmail;
    private String technicianName;
    private String technicianSurname;
    private Long ticketId;
    private String comment;
    private String status;
    private String priority;
    private String category;
    private String createdAt;
    private String dueAt;
    private String updatedAt;
    private String issueDescription;

    public EmailNotificationDTO() {
    }

    public EmailNotificationDTO(Tickets ticket, String comment) {
        this.technicianEmail = ticket.getAssignedTechnician().getEmail();
        this.normalUserEmail = ticket.getOwner();
        this.technicianName = ticket.getAssignedTechnician().getFirstName();
        this.technicianSurname = ticket.getAssignedTechnician().getLastName();
        this.ticketId = ticket.getId();
        this.comment = comment;
        this.status = ticket.getStatus().getStatus_name();
        this.priority = ticket.getPriority().getLevel();
        this.category = ticket.getCategory().getName();
        this.createdAt = ticket.getCreated_at().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.dueAt = ticket.getCreated_at().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        this.updatedAt = checkUpdateAtDate(ticket.getCreated_at());
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
