package za.gov.helpdesk.comment.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import za.gov.helpdesk.comment.dto.response.CommentResponse;
import za.gov.helpdesk.comment.dto.request.CreateCommentRequest;
import za.gov.helpdesk.comment.dto.request.UpdateCommentRequest;
import za.gov.helpdesk.users.model.User;

import java.util.List;

public interface CommentService {

    CommentResponse addComment(Long ticketId, CreateCommentRequest request, User actor);
    CommentResponse addReply(Long parentCommentId, CreateCommentRequest request, User actor);
    Page<CommentResponse> getComments(Long ticketId, Pageable pageable, User actor);
    List<CommentResponse> getReplies(Long commentId);
    CommentResponse updateComment(Long commentId, UpdateCommentRequest request, User actor);
    void deleteComment(Long commentId, User actor);
}
