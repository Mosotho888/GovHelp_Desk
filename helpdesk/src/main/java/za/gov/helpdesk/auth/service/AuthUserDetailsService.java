package za.gov.helpdesk.auth.service;

import za.gov.helpdesk.employee.exception.UserNotFoundException;
import za.gov.helpdesk.employee.model.Employees;
import za.gov.helpdesk.employee.repository.EmployeesRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@Slf4j
public class AuthUserDetailsService implements UserDetailsService {

    private final EmployeesRepository employeesRepository;


    public AuthUserDetailsService(EmployeesRepository employeesRepository) {
        this.employeesRepository = employeesRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        log.info("Attempting to load user by email: {}", email);

        Employees employeeDetails = getEmployeeByEmail(email);

        log.info("User found: {} with role: {}", employeeDetails.getEmail(), employeeDetails.getRole());

        return createUserFromEmployeeDetails(employeeDetails);
    }


    @NotNull
    private static UserDetails createUserFromEmployeeDetails(Employees employeeDetails) {
        return User.withUsername(employeeDetails.getEmail())
                .password(employeeDetails.getPassword())
                .roles(employeeDetails.getRole())
                .build();
    }


    private Employees getEmployeeByEmail(String email) {
        Optional<Employees> optionalEmployees = employeesRepository.findByEmail(email);
        
        if (optionalEmployees.isPresent()) {
            return optionalEmployees.get();
        }

        log.error("User not found for email: {}", email);
        throw new UserNotFoundException();
    }
}
