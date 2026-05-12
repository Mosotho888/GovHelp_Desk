package za.gov.helpdesk.auth.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import za.gov.helpdesk.users.dto.request.CreateUserRequest;
import za.gov.helpdesk.users.model.User;

import java.time.LocalDateTime;

@Component
public class RegisterRequestConverter implements Converter<CreateUserRequest, User> {
    private final PasswordEncoder passwordEncoder;

    public RegisterRequestConverter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public @NotNull User convert(CreateUserRequest request) {

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User();
        BeanUtils.copyProperties(request, user);

        user.setPasswordHash(encodedPassword);
        user.setRole(user.getRole());
        user.setCreatedAt(LocalDateTime.now());

        return user;
    }
}
