package com.loggingsystem.springjwtauth.ticket.dto;

import com.loggingsystem.springjwtauth.category.model.Category;
import com.loggingsystem.springjwtauth.priority.model.Priority;
import com.loggingsystem.springjwtauth.status.model.Status;
import com.loggingsystem.springjwtauth.ticketcomment.dto.CommentResponseDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssignedTicketsDTO {
    private Long id;
    private String ownerEmail;
    private Category category;
    private String description;
    private Status status;
    private Priority priority;
    private LocalDateTime createdAt;
    private List<CommentResponseDTO> comments;

    public AssignedTicketsDTO(TicketResponseDTO ticketResponse) {
        this.id = ticketResponse.getId();
        this.ownerEmail = ticketResponse.getOwnerEmail();
        this.category = ticketResponse.getCategory();
        this.description = ticketResponse.getDescription();
        this.status = ticketResponse.getStatus();
        this.priority = ticketResponse.getPriority();
        this.createdAt = ticketResponse.getCreatedAt();
        this.comments = ticketResponse.getComments();
    }
}
