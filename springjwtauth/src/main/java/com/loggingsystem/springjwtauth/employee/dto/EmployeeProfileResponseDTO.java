package com.loggingsystem.springjwtauth.employee.dto;

import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.ticket.dto.AssignedTicketsDTO;
import com.loggingsystem.springjwtauth.ticket.dto.SubmittedTicketsDTO;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class EmployeeProfileResponseDTO {

    private Long id;
    private String first_name;
    private String last_name;
    private String email;
    private String phone_number;
    private String role;
    private List<AssignedTicketsDTO> assignedTickets;
    private List<SubmittedTicketsDTO> submittedTickets;

    public EmployeeProfileResponseDTO(Employees employees){
        this.id = employees.getId();
        this.first_name = employees.getFirst_name();
        this.last_name = employees.getLast_name();
        this.email = employees.getEmail();
        this.phone_number = employees.getPhone_number();
        this.role = employees.getRole();
    }

    public void setAssignedTicketsBasedOnRole(String role, List<AssignedTicketsDTO> tickets) {
        if (role.equals("ADMIN") || role.equals("TECHNICIAN")) {
            this.assignedTickets = tickets;
        } else {
            this.submittedTickets = Collections.emptyList(); // Empty for normal users
        }
    }

    public void setSubmittedTicketsBasedOnRole(String role, List<SubmittedTicketsDTO> tickets) {
        if (role.equals("USER")) {
            this.submittedTickets = tickets;
        } else {
            this.assignedTickets = Collections.emptyList(); // Empty for normal users
        }
    }
}
