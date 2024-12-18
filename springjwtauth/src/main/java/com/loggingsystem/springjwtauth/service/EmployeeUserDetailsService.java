package com.loggingsystem.springjwtauth.service;

import com.loggingsystem.springjwtauth.exception.UsernameNotFoundException;
import com.loggingsystem.springjwtauth.model.Employees;
import com.loggingsystem.springjwtauth.repository.EmployeesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class EmployeeUserDetailsService implements UserDetailsService {
    private final EmployeesRepository employeesRepository;

    public EmployeeUserDetailsService(EmployeesRepository employeesRepository) {
        this.employeesRepository = employeesRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        log.info("Attempting to load user by email: {}", email);

        Optional<Employees> employee = employeesRepository.findByEmail(email);

        if (employee.isPresent()) {
            // Retrieve employee details
            Employees employeeDetails = employee.get();
            log.info("User found: {} with role: {}", employeeDetails.getEmail(), employeeDetails.getRole());

            // Return UserDetails object with email, password, and role
            return User.withUsername(employeeDetails.getEmail())
                    .password(employeeDetails.getPassword())
                    .roles(employeeDetails.getRole())
                    .build();

        }

        log.error("User not found for email: {}", email);
        throw new UsernameNotFoundException();

    }
}
