package com.loggingsystem.springjwtauth.ticketcomment.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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
