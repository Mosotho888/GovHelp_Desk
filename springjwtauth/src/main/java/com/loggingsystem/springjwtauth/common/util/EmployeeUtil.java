package com.loggingsystem.springjwtauth.common.util;

import com.loggingsystem.springjwtauth.employee.exception.UserNotFoundException;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.employee.repository.EmployeesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class EmployeeUtil {
    private final EmployeesRepository employeesRepository;

    public EmployeeUtil(EmployeesRepository employeesRepository) {
        this.employeesRepository = employeesRepository;
    }

    public Employees getEmployee(Long id) {
        Optional<Employees> optionalEmployees = employeesRepository.findById(id);

        if (optionalEmployees.isPresent()) {
            log.info("Employee found with ID: {}", id);
            return  optionalEmployees.get();
        }

        log.warn("No employee found with ID: {}", id);
        throw new UserNotFoundException();
    }

    public Employees getEmployeeByEmail(String email) {
        Optional<Employees> employeeProfile = employeesRepository.findByEmail(email);

        if (employeeProfile.isPresent()) {
            log.info("Employee found with email: {}", email);

            return employeeProfile.get();
        }

        log.warn("No employee found with email: {}", email);
        throw new UserNotFoundException();
    }

}
