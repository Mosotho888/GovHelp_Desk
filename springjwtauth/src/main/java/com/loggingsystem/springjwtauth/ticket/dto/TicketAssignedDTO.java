package com.loggingsystem.springjwtauth.ticket.dto;

import com.loggingsystem.springjwtauth.category.model.Category;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.priority.model.Priority;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import com.loggingsystem.springjwtauth.ticketcomment.dto.CommentResponseDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TicketAssignedDTO {
    private Long id;
    private String status;
    private String description;
    private String resolution;
    private String attachmentsUrl;
    private String ownerEmail;
    private Category category;
    private Priority priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponseDTO> comments;

    public TicketAssignedDTO(Tickets ticket) {
        this.id = ticket.getId();
        this.status = ticket.getStatus().getStatus_name();
        this.description = ticket.getDescription();
        this.resolution = ticket.getResolution();
        this.attachmentsUrl = ticket.getAttachments();
        this.ownerEmail = ticket.getOwner();
        this.category = ticket.getCategory();
        this.priority = ticket.getPriority();
        this.createdAt = ticket.getCreated_at();
        this.updatedAt = ticket.getUpdated_at();
        this.comments = ticket.getComments().stream().map(CommentResponseDTO::new).toList();
    }
}
