package za.gov.helpdesk.users.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import za.gov.helpdesk.users.dto.response.UserResponse;
import za.gov.helpdesk.users.model.User;

@Component
public class EmployeeToEmployeeResponseConverter implements Converter<User, UserResponse> {
    @Override
    public @NotNull UserResponse convert(@NotNull User user) {

        return toResponse(user);
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .phone(user.getPhone())
                .timezone(user.getTimezone())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
