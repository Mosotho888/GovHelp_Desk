package za.gov.helpdesk.comment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import za.gov.helpdesk.comment.dto.response.CommentResponse;
import za.gov.helpdesk.comment.model.Comment;
import za.gov.helpdesk.users.mapper.UserMapper;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface CommentMapper {

    @Mapping(source = "ticket.id", target = "ticketId")
    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(target = "replies", expression = "java(java.util.List.of())")
    CommentResponse toCommentResponse(Comment comment);
}
