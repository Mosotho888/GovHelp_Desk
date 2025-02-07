package com.loggingsystem.springjwtauth.ticketcomment.dto;

import com.loggingsystem.springjwtauth.ticketcomment.model.TicketComments;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
public class CommentResponseDTO {
    private Long id;
    private Long commenterId;
    private String commenterEmail;
    private String role;
    private String comment;
    private LocalDateTime created_at;

    public CommentResponseDTO(TicketComments ticketComments) {
        this.id = ticketComments.getId();
        this.commenterId = ticketComments.getCommenter().getId();
        this.commenterEmail = ticketComments.getCommenter().getEmail();
        this.role = ticketComments.getCommenter().getRole();
        this.comment = ticketComments.getComment();
        this.created_at = ticketComments.getCreated_at();
    }
}
