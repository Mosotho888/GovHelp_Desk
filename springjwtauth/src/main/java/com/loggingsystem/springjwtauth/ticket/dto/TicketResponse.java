package com.loggingsystem.springjwtauth.ticket.dto;

import com.loggingsystem.springjwtauth.employee.dto.EmployeeResponse;
import com.loggingsystem.springjwtauth.status.model.Status;
import com.loggingsystem.springjwtauth.ticketcomment.dto.CommentResponse;
import com.loggingsystem.springjwtauth.category.model.Category;
import com.loggingsystem.springjwtauth.priority.model.Priority;

import java.time.LocalDateTime;
import java.util.List;

public record TicketResponse(
        Long id,
        EmployeeResponse assignedTechnician,
        Status status,
        String description,
        String ownerEmail,
        Category category,
        Priority priority,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CommentResponse> comments
) { }
