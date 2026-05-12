package za.gov.helpdesk.comment.service;

import za.gov.helpdesk.comment.dto.response.CommentResponse;
import za.gov.helpdesk.comment.model.Comment;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import za.gov.helpdesk.users.dto.response.UserResponse;

import java.util.List;

@Component
public class TicketCommentsToCommentResponseConverter implements Converter<Comment, CommentResponse> {
    @Override
    public @NotNull CommentResponse convert(Comment c) {

        return CommentResponse.builder()
                .id(c.getId())
                .ticketId(c.getTicket().getId())
                .author(UserResponse.builder()
                        .id(c.getAuthor().getId())
                        .name(c.getAuthor().getName())
                        .email(c.getAuthor().getEmail())
                        .role(c.getAuthor().getRole())
                        .build())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .body(c.getBody())
                .internal(c.isInternal())
                .type(c.getType())
                .createdAt(c.getCreatedAt())
                .replies(List.of())
                .build();
    }
}
