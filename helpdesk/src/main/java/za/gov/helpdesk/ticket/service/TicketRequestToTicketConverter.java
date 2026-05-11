package za.gov.helpdesk.ticket.service;

import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.category.service.CategoryService;
import za.gov.helpdesk.common.util.EmployeeUtil;
import za.gov.helpdesk.priority.model.Priority;
import za.gov.helpdesk.priority.service.PriorityService;
import za.gov.helpdesk.status.model.Status;
import za.gov.helpdesk.status.service.StatusService;
import za.gov.helpdesk.ticket.dto.CreateTicketRequest;
import za.gov.helpdesk.ticket.model.Ticket;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TicketRequestToTicketConverter implements Converter<CreateTicketRequest, Ticket> {
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
    public @NotNull Ticket convert(CreateTicketRequest createTicketRequest) {
        za.gov.helpdesk.users.model.User assignedTechnician = employeeUtil.getEmployee(createTicketRequest.assignedTechnicianId());
        Status assignedStatus = statusService.getStatus(createTicketRequest.statusId());
        Category assignedCategory = categoryService.getCategory(createTicketRequest.categoryId());
        Priority assignedPriority = priorityService.getPriority(createTicketRequest.priorityId());

        Ticket ticket = new Ticket();

        ticket.setAssignedTechnician(assignedTechnician);
        ticket.setStatus(assignedStatus);
        ticket.setDescription(createTicketRequest.description());
        ticket.setCategory(assignedCategory);
        ticket.setPriority(assignedPriority);

        return ticket;
    }
}
