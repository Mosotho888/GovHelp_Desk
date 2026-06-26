package za.gov.helpdesk.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import za.gov.helpdesk.comment.model.Comment;

import lombok.Data;

@Data
public class CreateCommentRequest {

    @NotBlank(message = "Comment body is required")
    @Size(max = 10_000, message = "Comment must not exceed 10,000 characters")
    private String body;

    private boolean internal;

    private Comment.CommentType type = Comment.CommentType.REPLY;
}
