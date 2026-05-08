package za.gov.helpdesk.employee.service;

import za.gov.helpdesk.employee.dto.RegisterRequest;
import za.gov.helpdesk.employee.dto.EmployeeProfileResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import za.gov.helpdesk.employee.dto.EmployeeResponse;

import java.util.List;

public interface EmployeeService {
    ResponseEntity<List<EmployeeProfileResponse>> getAllEmployees(Pageable pageable);
    ResponseEntity<EmployeeProfileResponse> getEmployeeById(Long employeeId);
    ResponseEntity<EmployeeProfileResponse> getEmployeeProfileByEmail(String email);
    ResponseEntity<List<EmployeeProfileResponse>> getAllTechnicians(Pageable pageable);
    ResponseEntity<Void> deleteEmployeeById(Long employeeId);
    EmployeeResponse registerEmployee(RegisterRequest registerRequest);
}
