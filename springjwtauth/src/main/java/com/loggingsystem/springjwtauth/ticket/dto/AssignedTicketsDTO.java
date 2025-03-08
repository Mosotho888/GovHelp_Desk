package com.loggingsystem.springjwtauth.ticket.dto;

import com.loggingsystem.springjwtauth.category.model.Category;
import com.loggingsystem.springjwtauth.priority.model.Priority;
import com.loggingsystem.springjwtauth.status.model.Status;
import com.loggingsystem.springjwtauth.ticketcomment.dto.CommentResponse;
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
    private List<CommentResponse> comments;

    public AssignedTicketsDTO(TicketResponse ticketResponse) {
        this.id = ticketResponse.id();
        this.ownerEmail = ticketResponse.ownerEmail();
        this.category = ticketResponse.category();
        this.description = ticketResponse.description();
        this.status = ticketResponse.status();
        this.priority = ticketResponse.priority();
        this.createdAt = ticketResponse.createdAt();
        this.comments = ticketResponse.comments();
    }
}
