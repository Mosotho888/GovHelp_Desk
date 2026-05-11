package za.gov.helpdesk.comment.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import za.gov.helpdesk.comment.dto.CommentResponse;
import za.gov.helpdesk.comment.dto.CreateCommentRequest;
import za.gov.helpdesk.comment.dto.UpdateCommentRequest;

import java.util.List;

public interface CommentService {

    CommentResponse addComment(Long ticketId, CreateCommentRequest request);
    CommentResponse addReply(Long parentCommentId, CreateCommentRequest request);
    Page<CommentResponse> getComments(Long ticketId, Pageable pageable);
    List<CommentResponse> getReplies(Long commentId);
    CommentResponse updateComment(Long commentId, UpdateCommentRequest request);
    void deleteComment(Long commentId);
}
