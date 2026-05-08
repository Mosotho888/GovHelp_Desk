package za.gov.helpdesk.common.util;

import za.gov.helpdesk.users.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class EmployeeUtil {
    private final za.gov.helpdesk.users.repository.UserRepository userRepository;

    public EmployeeUtil(za.gov.helpdesk.users.repository.UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public za.gov.helpdesk.users.model.User getEmployee(Long id) {
        Optional<za.gov.helpdesk.users.model.User> optionalEmployees = userRepository.findById(id);

        if (optionalEmployees.isPresent()) {
            log.info("Employee found with ID: {}", id);
            return  optionalEmployees.get();
        }

        log.warn("No users found with ID: {}", id);
        throw new UserNotFoundException();
    }

    public za.gov.helpdesk.users.model.User getEmployeeByEmail(String email) {
        Optional<za.gov.helpdesk.users.model.User> employeeProfile = userRepository.findByEmail(email);

        if (employeeProfile.isPresent()) {
            log.info("Employee found with email: {}", email);

            return employeeProfile.get();
        }

        log.warn("No users found with email: {}", email);
        throw new UserNotFoundException();
    }

}
