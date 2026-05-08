package za.gov.helpdesk.users.service;

import za.gov.helpdesk.users.dto.RegisterRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface EmployeeService {
    ResponseEntity<List<za.gov.helpdesk.users.dto.UserProfileResponse>> getAllEmployees(Pageable pageable);
    ResponseEntity<za.gov.helpdesk.users.dto.UserProfileResponse> getEmployeeById(Long employeeId);
    ResponseEntity<za.gov.helpdesk.users.dto.UserProfileResponse> getEmployeeProfileByEmail(String email);
    ResponseEntity<List<za.gov.helpdesk.users.dto.UserProfileResponse>> getAllTechnicians(Pageable pageable);
    ResponseEntity<Void> deleteEmployeeById(Long employeeId);
    za.gov.helpdesk.users.dto.UserResponse registerEmployee(RegisterRequest registerRequest);
}
