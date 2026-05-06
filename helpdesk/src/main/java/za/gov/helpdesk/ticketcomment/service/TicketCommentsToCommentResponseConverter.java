package za.gov.helpdesk.ticketcomment.service;

import za.gov.helpdesk.ticketcomment.dto.CommentResponse;
import za.gov.helpdesk.ticketcomment.model.TicketComments;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TicketCommentsToCommentResponseConverter implements Converter<TicketComments, CommentResponse> {
    @Override
    public @NotNull CommentResponse convert(TicketComments ticketComment) {

        return new CommentResponse(ticketComment.getId(), ticketComment.getCommenter().getId(),
                ticketComment.getCommenter().getEmail(), ticketComment.getCommenter().getRole(), ticketComment.getComment(), ticketComment.getCreated_at());
    }
}
