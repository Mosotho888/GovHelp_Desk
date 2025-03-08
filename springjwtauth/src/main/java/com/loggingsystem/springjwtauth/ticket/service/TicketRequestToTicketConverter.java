package com.loggingsystem.springjwtauth.ticket.service;

import com.loggingsystem.springjwtauth.category.model.Category;
import com.loggingsystem.springjwtauth.category.service.CategoryService;
import com.loggingsystem.springjwtauth.common.util.EmployeeUtil;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.priority.model.Priority;
import com.loggingsystem.springjwtauth.priority.service.PriorityService;
import com.loggingsystem.springjwtauth.status.model.Status;
import com.loggingsystem.springjwtauth.status.service.StatusService;
import com.loggingsystem.springjwtauth.ticket.dto.TicketRequest;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TicketRequestToTicketConverter implements Converter<TicketRequest, Tickets> {
    private final EmployeeUtil employeeUtil;
    private final PriorityService priorityService;
    private final CategoryService categoryService;
    private final StatusService statusService;

    public TicketRequestToTicketConverter(EmployeeUtil employeeUtil, PriorityService priorityService, CategoryService categoryService, StatusService statusService) {
        this.employeeUtil = employeeUtil;
        this.priorityService = priorityService;
        this.categoryService = categoryService;
        this.statusService = statusService;
    }

    @Override
    public @NotNull Tickets convert(TicketRequest ticketRequest) {
        Employees assignedTechnician = employeeUtil.getEmployee(ticketRequest.assignedTechnicianId());
        Status assignedStatus = statusService.getStatus(ticketRequest.statusId());
        Category assignedCategory = categoryService.getCategory(ticketRequest.categoryId());
        Priority assignedPriority = priorityService.getPriority(ticketRequest.priorityId());

        Tickets ticket = new Tickets();

        ticket.setAssignedTechnician(assignedTechnician);
        ticket.setStatus(assignedStatus);
        ticket.setDescription(ticketRequest.description());
        ticket.setCategory(assignedCategory);
        ticket.setPriority(assignedPriority);

        return ticket;
    }
}
