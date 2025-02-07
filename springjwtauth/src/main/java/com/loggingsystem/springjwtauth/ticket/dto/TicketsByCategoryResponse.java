package com.loggingsystem.springjwtauth.ticket.dto;

import com.loggingsystem.springjwtauth.employee.dto.EmployeeResponseDTO;
import com.loggingsystem.springjwtauth.priority.model.Priority;
import com.loggingsystem.springjwtauth.status.model.Status;
import com.loggingsystem.springjwtauth.ticketcomment.dto.CommentResponseDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TicketsByCategoryResponse {
    private Long id;
    private String ownerEmail;
    private EmployeeResponseDTO assignedTechnician;
    private Status status;
    private Priority priority;
    private String description;
    private String resolution;
    private String attachmentsUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponseDTO> comments;

    public TicketsByCategoryResponse(TicketResponseDTO ticketResponse) {
        this.id = ticketResponse.getId();
        this.ownerEmail = ticketResponse.getOwnerEmail();
        this.assignedTechnician = ticketResponse.getAssignedTechnician();
        this.status = ticketResponse.getStatus();
        this.priority = ticketResponse.getPriority();
        this.description = ticketResponse.getDescription();
        this.resolution = ticketResponse.getResolution();
        this.attachmentsUrl = ticketResponse.getAttachmentsUrl();
        this.createdAt = ticketResponse.getCreatedAt();
        this.comments = ticketResponse.getComments();
    }
}
