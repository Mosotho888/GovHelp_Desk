package com.loggingsystem.springjwtauth.ticketcomment.service;

import com.loggingsystem.springjwtauth.ticketcomment.dto.CommentResponse;
import com.loggingsystem.springjwtauth.ticketcomment.model.TicketComments;
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
