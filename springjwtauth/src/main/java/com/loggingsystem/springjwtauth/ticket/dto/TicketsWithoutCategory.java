package com.loggingsystem.springjwtauth.ticket.dto;

import com.loggingsystem.springjwtauth.employee.dto.EmployeeResponse;
import com.loggingsystem.springjwtauth.priority.model.Priority;
import com.loggingsystem.springjwtauth.status.model.Status;
import com.loggingsystem.springjwtauth.ticketcomment.dto.CommentResponse;

import java.time.LocalDateTime;
import java.util.List;

public record TicketsWithoutCategory (
        Long id,
        String ownerEmail,
        EmployeeResponse assignedTechnician,
        Status status,
        Priority priority,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CommentResponse> comments
){ }
