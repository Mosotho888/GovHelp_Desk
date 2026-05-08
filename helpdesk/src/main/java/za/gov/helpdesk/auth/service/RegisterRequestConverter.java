package za.gov.helpdesk.auth.service;

import za.gov.helpdesk.users.dto.RegisterRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RegisterRequestConverter implements Converter<RegisterRequest, za.gov.helpdesk.users.model.User> {
    private final PasswordEncoder passwordEncoder;

    public RegisterRequestConverter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public @NotNull za.gov.helpdesk.users.model.User convert(RegisterRequest registerRequest) {

        String encodedPassword = passwordEncoder.encode(registerRequest.password());

        za.gov.helpdesk.users.model.User employee = new za.gov.helpdesk.users.model.User();
        BeanUtils.copyProperties(registerRequest, employee);

        employee.setPassword(encodedPassword);
        employee.setRole(employee.getRole());
        employee.setCreatedAt(LocalDateTime.now());

        return employee;
    }
}
