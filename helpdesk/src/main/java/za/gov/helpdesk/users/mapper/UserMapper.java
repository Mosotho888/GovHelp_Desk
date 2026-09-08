package za.gov.helpdesk.users.mapper;

import org.mapstruct.Mapper;

import za.gov.helpdesk.users.dto.response.UserResponse;
import za.gov.helpdesk.users.model.User;

@Mapper(componentModel = "spring")
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface UserMapper {

    UserResponse toUserResponse(User user);
}
