package com.loggingsystem.springjwtauth.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
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
