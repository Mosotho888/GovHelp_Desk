package com.loggingsystem.springjwtauth.ticket.dto;

import com.loggingsystem.springjwtauth.category.model.Category;
import com.loggingsystem.springjwtauth.employee.dto.EmployeeResponse;
import com.loggingsystem.springjwtauth.priority.model.Priority;
import com.loggingsystem.springjwtauth.status.model.Status;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubmittedTicketsDTO {
    private Long id;
    private EmployeeResponse assignedTechnician;
    private Category category;
    private String description;
    private Status status;
    private Priority priority;
    private LocalDateTime createdAt;

    public SubmittedTicketsDTO(TicketResponseDTO ticketResponse) {
        this.id = ticketResponse.getId();
        this.assignedTechnician = ticketResponse.getAssignedTechnician();
        this.category = ticketResponse.getCategory();
        this.description = ticketResponse.getDescription();
        this.status = ticketResponse.getStatus();
        this.priority = ticketResponse.getPriority();
        this.createdAt = ticketResponse.getCreatedAt();
    }
}
