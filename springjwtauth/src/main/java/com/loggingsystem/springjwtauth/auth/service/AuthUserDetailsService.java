package com.loggingsystem.springjwtauth.auth.service;

import com.loggingsystem.springjwtauth.employee.exception.UserNotFoundException;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.employee.repository.EmployeesRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for loading user details for authentication.
 * Implements Spring Security's {@link UserDetailsService} interface to retrieve user data
 * from the database and build {@link UserDetails} objects.
 */

@Service
@Slf4j
public class AuthUserDetailsService implements UserDetailsService {

    private final EmployeesRepository employeesRepository;

    /**
     * Constructor for injecting the {@link EmployeesRepository}.
     *
     * @param employeesRepository the repository used to access employee data.
     */
    public AuthUserDetailsService(EmployeesRepository employeesRepository) {
        this.employeesRepository = employeesRepository;
    }

    /**
     * Loads a user's details based on their email for authentication purposes.
     *
     * @param email the email of the user to be authenticated.
     * @return the {@link UserDetails} object containing user credentials and roles.
     * @throws UserNotFoundException if the user is not found in the database.
     */
    @Override
    public UserDetails loadUserByUsername(String email) {
        log.info("Attempting to load user by email: {}", email);

        Employees employeeDetails = getEmployeeByEmail(email);

        log.info("User found: {} with role: {}", employeeDetails.getEmail(), employeeDetails.getRole());

        return createUserFromEmployeeDetails(employeeDetails);
    }

    /**
     * Converts an {@link Employees} object into a {@link UserDetails} object for authentication.
     *
     * @param employeeDetails the employee details retrieved from the database.
     * @return a {@link UserDetails} object containing username, password, and roles.
     */
    @NotNull
    private static UserDetails createUserFromEmployeeDetails(Employees employeeDetails) {
        return User.withUsername(employeeDetails.getEmail())
                .password(employeeDetails.getPassword())
                .roles(employeeDetails.getRole())
                .build();
    }

    /**
     * Retrieves an employee by their email from the database.
     *
     * @param email the email of the employee to retrieve.
     * @return an {@link Employees} object if found.
     * @throws UserNotFoundException if no user is found for the given email.
     */
    private Employees getEmployeeByEmail(String email) {
        Optional<Employees> optionalEmployees = employeesRepository.findByEmail(email);
        
        if (optionalEmployees.isPresent()) {
            return optionalEmployees.get();
        }

        log.error("User not found for email: {}", email);
        throw new UserNotFoundException();
    }
}
