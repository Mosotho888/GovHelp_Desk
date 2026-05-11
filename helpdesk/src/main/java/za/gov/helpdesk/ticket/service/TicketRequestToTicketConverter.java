package za.gov.helpdesk.ticket.service;

import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.category.service.CategoryService;
import za.gov.helpdesk.common.util.EmployeeUtil;
import za.gov.helpdesk.priority.model.Priority;
import za.gov.helpdesk.priority.service.PriorityService;
import za.gov.helpdesk.status.model.Status;
import za.gov.helpdesk.status.service.StatusService;
import za.gov.helpdesk.ticket.dto.TicketRequest;
import za.gov.helpdesk.ticket.model.Ticket;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TicketRequestToTicketConverter implements Converter<TicketRequest, Ticket> {
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
    public @NotNull Ticket convert(TicketRequest ticketRequest) {
        za.gov.helpdesk.users.model.User assignedTechnician = employeeUtil.getEmployee(ticketRequest.assignedTechnicianId());
        Status assignedStatus = statusService.getStatus(ticketRequest.statusId());
        Category assignedCategory = categoryService.getCategory(ticketRequest.categoryId());
        Priority assignedPriority = priorityService.getPriority(ticketRequest.priorityId());

        Ticket ticket = new Ticket();

        ticket.setAssignedTechnician(assignedTechnician);
        ticket.setStatus(assignedStatus);
        ticket.setDescription(ticketRequest.description());
        ticket.setCategory(assignedCategory);
        ticket.setPriority(assignedPriority);

        return ticket;
    }
}
