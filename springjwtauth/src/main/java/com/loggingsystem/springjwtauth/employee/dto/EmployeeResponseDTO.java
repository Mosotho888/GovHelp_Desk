package com.loggingsystem.springjwtauth.employee.dto;

import com.loggingsystem.springjwtauth.employee.model.Employees;
import lombok.Data;

@Data
public class EmployeeResponseDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;

    public EmployeeResponseDTO(Employees employee) {
        this.id = employee.getId();
        this.firstName = employee.getFirst_name();
        this.lastName = employee.getLast_name();
        this.email = employee.getEmail();
        this.role = employee.getRole();
    }
}
