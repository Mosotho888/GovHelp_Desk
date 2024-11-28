package com.loggingsystem.springjwtauth.service;

import com.loggingsystem.springjwtauth.model.Employees;
import com.loggingsystem.springjwtauth.repository.EmployeesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Optional<Employees> employee = employeesRepository.findByEmail(email);

        if (employee.isPresent()) {
            // Retrieve employee details
            Employees employeeDetails = employee.get();

            // Return UserDetails object with email, password, and role
            return User.withUsername(employeeDetails.getEmail())
                    .password(employeeDetails.getPassword())
                    .roles(employeeDetails.getRole())
                    .build();

        }

        throw new UsernameNotFoundException("User not found with email: " + email);

    }
}
