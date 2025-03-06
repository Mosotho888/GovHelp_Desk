package com.loggingsystem.springjwtauth.employee.dto;

import com.loggingsystem.springjwtauth.employee.model.Employees;
import lombok.Data;

@Data
public class EmployeeResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;

    public EmployeeResponse(Employees employee) {
        this.id = employee.getId();
        this.firstName = employee.getFirstName();
        this.lastName = employee.getLastName();
        this.email = employee.getEmail();
        this.role = employee.getRole();
    }
}
