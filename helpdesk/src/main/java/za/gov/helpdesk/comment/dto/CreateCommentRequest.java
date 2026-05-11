package za.gov.helpdesk.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import za.gov.helpdesk.comment.model.Comment;

@Data
public class CreateCommentRequest {

    @NotBlank(message = "Comment body is required")
    @Size(max = 10000, message = "Comment must not exceed 10,000 characters")
    private String body;

    private boolean internal = false;

    private Comment.CommentType type = Comment.CommentType.REPLY;

}
