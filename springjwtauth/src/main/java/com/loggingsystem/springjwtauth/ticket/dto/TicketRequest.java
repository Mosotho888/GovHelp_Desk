package com.loggingsystem.springjwtauth.ticket.dto;

public record TicketRequest(
        Long assignedTechnicianId,
        Long statusId,
        String description,
        Long categoryId,
        Long priorityId
) { }
