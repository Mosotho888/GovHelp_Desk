package com.loggingsystem.springjwtauth.ticket.dto;

import com.loggingsystem.springjwtauth.employee.dto.EmployeeResponseDTO;
import com.loggingsystem.springjwtauth.status.model.Status;
import com.loggingsystem.springjwtauth.ticketcomment.dto.CommentResponseDTO;
import com.loggingsystem.springjwtauth.category.model.Category;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.priority.model.Priority;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Getter
@Setter
public class TicketResponseDTO {
    private Long id;
    private EmployeeResponseDTO assignedTechnician;
    private Status status;
    private String description;
    private String resolution;
    private String attachmentsUrl;
    private String ownerEmail;
    private Category category;
    private Priority priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponseDTO> comments;

    public TicketResponseDTO(Tickets tickets) {
        this.id = tickets.getId();
        this.assignedTechnician = mapToEmployeeResponseDTO(tickets.getAssignedTechnician());
        this.status = tickets.getStatus();
        this.description = tickets.getDescription();
        this.resolution = tickets.getResolution();
        this.attachmentsUrl = tickets.getAttachments();
        this.ownerEmail = tickets.getOwner();
        this.category = tickets.getCategory();
        this.priority = tickets.getPriority();
        this.createdAt = tickets.getCreated_at();
        this.updatedAt = tickets.getUpdated_at();
        this.comments = tickets.getComments().stream().map(CommentResponseDTO::new).toList();
    }

    private EmployeeResponseDTO mapToEmployeeResponseDTO(Employees assignedTechnician) {
        return new EmployeeResponseDTO(assignedTechnician);
    }
}
