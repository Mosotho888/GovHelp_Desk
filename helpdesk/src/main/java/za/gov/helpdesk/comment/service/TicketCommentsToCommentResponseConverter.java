package za.gov.helpdesk.comment.service;

import za.gov.helpdesk.comment.dto.CommentResponse;
import za.gov.helpdesk.comment.model.Comment;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TicketCommentsToCommentResponseConverter implements Converter<Comment, CommentResponse> {
    @Override
    public @NotNull CommentResponse convert(Comment ticketComment) {

        return new CommentResponse(ticketComment.getId(), ticketComment.getCommenter().getId(),
                ticketComment.getCommenter().getEmail(), ticketComment.getCommenter().getRole(), ticketComment.getComment(), ticketComment.getCreated_at());
    }
}
