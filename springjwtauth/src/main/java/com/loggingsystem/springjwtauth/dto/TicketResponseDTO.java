package com.loggingsystem.springjwtauth.dto;

import com.loggingsystem.springjwtauth.model.Category;
import com.loggingsystem.springjwtauth.model.Employees;
import com.loggingsystem.springjwtauth.model.Priority;
import com.loggingsystem.springjwtauth.model.Tickets;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Getter
@Setter
public class TicketResponseDTO {
    private Long id;
    private Employees assignedTechnician;
    private String status;
    private String description;
    private String resolution;
    private String attachmentsUrl;
    private String ownerEmail;
    private Category category;
    private Priority priority;
    private LocalDateTime createdAt;
    private List<CommentResponseDTO> comments;

    public TicketResponseDTO(Tickets tickets) {
        this.id = tickets.getId();
        this.assignedTechnician = tickets.getAssignedTechnician();
        this.status = tickets.getStatus().getStatus_name();
        this.description = tickets.getDescription();
        this.resolution = tickets.getResolution();
        this.attachmentsUrl = tickets.getAttachments();
        this.ownerEmail = tickets.getOwner();
        this.category = tickets.getCategory();
        this.priority = tickets.getPriority();
        this.createdAt = tickets.getCreated_at();
        this.comments = tickets.getComments().stream().map(CommentResponseDTO::new).toList();
    }
}
