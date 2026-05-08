package za.gov.helpdesk.users.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class EmployeeToEmployeeResponseConverter implements Converter<za.gov.helpdesk.users.model.User, za.gov.helpdesk.users.dto.UserResponse> {
    @Override
    public @NotNull za.gov.helpdesk.users.dto.UserResponse convert(za.gov.helpdesk.users.model.User employee) {

        return new za.gov.helpdesk.users.dto.UserResponse(employee.getId(), employee.getFirstName(),
                employee.getLastName(), employee.getEmail(), employee.getPhoneNumber(), employee.getRole());
    }
}
