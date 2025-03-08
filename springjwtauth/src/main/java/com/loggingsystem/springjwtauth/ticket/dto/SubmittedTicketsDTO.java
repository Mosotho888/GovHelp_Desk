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

    public SubmittedTicketsDTO(TicketResponse ticketResponse) {
        this.id = ticketResponse.id();
        this.assignedTechnician = ticketResponse.assignedTechnician();
        this.category = ticketResponse.category();
        this.description = ticketResponse.description();
        this.status = ticketResponse.status();
        this.priority = ticketResponse.priority();
        this.createdAt = ticketResponse.createdAt();
    }
}
