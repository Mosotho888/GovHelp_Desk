package za.gov.helpdesk.comment.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import za.gov.helpdesk.comment.dto.request.CreateCommentRequest;
import za.gov.helpdesk.comment.dto.request.UpdateCommentRequest;
import za.gov.helpdesk.comment.dto.response.CommentResponse;
import za.gov.helpdesk.users.model.User;

public interface CommentService {

    CommentResponse addComment(Long ticketId, CreateCommentRequest request, User actor);

    CommentResponse addReply(Long parentCommentId, CreateCommentRequest request, User actor);

    Page<CommentResponse> getComments(Long ticketId, Pageable pageable, User actor);

    List<CommentResponse> getReplies(Long commentId, User actor);

    CommentResponse updateComment(Long commentId, UpdateCommentRequest request, User actor);

    void deleteComment(Long commentId, User actor);
}
