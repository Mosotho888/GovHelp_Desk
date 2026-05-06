package za.gov.helpdesk.employee.dto;

public record EmployeeResponse (
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String role
) { }
