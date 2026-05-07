package za.gov.helpdesk.auth.service;

import za.gov.helpdesk.auth.dto.LoginRequest;
import za.gov.helpdesk.auth.dto.AuthResponse;
import za.gov.helpdesk.auth.dto.RegisterRequest;
import za.gov.helpdesk.employee.dto.EmployeeResponse;
import org.springframework.http.ResponseEntity;

public interface AuthService {

    ResponseEntity<AuthResponse> login(LoginRequest loginRequest);
    ResponseEntity<EmployeeResponse> registerEmployee(RegisterRequest registerRequest);
}
