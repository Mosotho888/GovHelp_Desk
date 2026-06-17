package za.gov.helpdesk.comment.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import za.gov.helpdesk.comment.model.Comment;
import za.gov.helpdesk.users.dto.response.UserResponse;

@Data
@Builder
public class CommentResponse {
    private Long id;
    private Long ticketId;
    private UserResponse author;
    private Long parentId;
    private String body;
    private boolean internal;
    private Comment.CommentType type;
    private LocalDateTime createdAt;
    private List<CommentResponse> replies;
}
