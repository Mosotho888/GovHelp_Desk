package com.loggingsystem.springjwtauth.auth.service;

import com.loggingsystem.springjwtauth.auth.dto.SignUpRequest;
import com.loggingsystem.springjwtauth.auth.dto.LoginResponse;
import com.loggingsystem.springjwtauth.employee.dto.EmployeeResponse;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import org.springframework.http.ResponseEntity;

public interface AuthenticationService {

    ResponseEntity<LoginResponse> login(SignUpRequest signUpRequest);
    ResponseEntity<EmployeeResponse> registerEmployee(Employees newEmployee);
}
