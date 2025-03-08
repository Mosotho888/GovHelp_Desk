package com.loggingsystem.springjwtauth.employee.service;

import com.loggingsystem.springjwtauth.employee.dto.EmployeeProfileResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface EmployeeService {
    ResponseEntity<List<EmployeeProfileResponse>> getAllEmployees(Pageable pageable);
    ResponseEntity<EmployeeProfileResponse> getEmployeeById(Long employeeId);
    ResponseEntity<EmployeeProfileResponse> getEmployeeProfileByEmail(String email);
    ResponseEntity<List<EmployeeProfileResponse>> getAllTechnicians(Pageable pageable);
    ResponseEntity<Void> deleteEmployeeById(Long employeeId);
}
