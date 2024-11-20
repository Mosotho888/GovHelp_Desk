package com.loggingsystem.springjwtauth.dto;

import com.loggingsystem.springjwtauth.model.TicketComments;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Data
public class CommentDTO {
    private String comments;
    private Long comment_id;

    public CommentDTO() {
    }

    public CommentDTO(String comments, Long comment_id) {
        this.comments = comments;
        this.comment_id = comment_id;
    }

}
