package za.gov.helpdesk.employee.dto;

import lombok.Builder;
import lombok.Data;
import za.gov.helpdesk.employee.model.Employees;

import java.time.LocalDateTime;

@Data
@Builder
public class EmployeeResponse {
    private Long id;
    private String name;
    private String email;
    private Employees.Role role;
    private String phone;
    private String timezone;
    private boolean active;
    private LocalDateTime createdAt;
}
