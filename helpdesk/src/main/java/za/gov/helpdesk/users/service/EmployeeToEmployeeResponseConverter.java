package za.gov.helpdesk.employee.service;

import za.gov.helpdesk.employee.dto.EmployeeResponse;
import za.gov.helpdesk.employee.model.Employees;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class EmployeeToEmployeeResponseConverter implements Converter<Employees, EmployeeResponse> {
    @Override
    public @NotNull EmployeeResponse convert(Employees employee) {

        return new EmployeeResponse(employee.getId(), employee.getFirstName(),
                employee.getLastName(), employee.getEmail(), employee.getPhoneNumber(), employee.getRole());
    }
}
