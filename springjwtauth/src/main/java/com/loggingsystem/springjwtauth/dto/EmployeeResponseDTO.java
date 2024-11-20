package com.loggingsystem.springjwtauth.dto;

import com.loggingsystem.springjwtauth.model.Employees;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmployeeResponseDTO {

    private Long id;
    private String password;
    private String first_name;
    private String last_name;
    private String email;
    private String phone_number;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private String role;

    public EmployeeResponseDTO(Employees employees){
        this.id = employees.getId();
        this.password = employees.getPassword();
        this.first_name = employees.getFirst_name();
        this.last_name = employees.getLast_name();
        this.email = maskEmail(employees.getEmail());
        this.phone_number = maskPhoneNumber(employees.getPhone_number());
        this.created_at = employees.getCreated_at();
        this.updated_at = employees.getUpdated_at();
        this.role = employees.getRole();
    }

    private String maskEmail(String email) {
        return email.replaceAll("(?<=.{1}).(?=.*@)", "*");
    }

    private String maskPhoneNumber(String phoneNumber) {
        return phoneNumber.replaceAll("\\d(?=\\d{4})", "*");
    }
}
