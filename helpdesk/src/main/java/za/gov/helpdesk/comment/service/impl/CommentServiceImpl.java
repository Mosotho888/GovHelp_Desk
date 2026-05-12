package za.gov.helpdesk.comment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.comment.dto.response.CommentResponse;
import za.gov.helpdesk.comment.dto.request.CreateCommentRequest;
import za.gov.helpdesk.comment.dto.request.UpdateCommentRequest;
import za.gov.helpdesk.comment.model.Comment;
import za.gov.helpdesk.comment.repository.CommentRepository;
import za.gov.helpdesk.comment.service.CommentService;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.users.dto.response.UserResponse;
import za.gov.helpdesk.users.exception.UserNotFoundException;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    private static final int EDIT_WINDOW_MINUTES = 15;

    @Override
    @Transactional
    public CommentResponse addComment(Long ticketId, CreateCommentRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        User author = getCurrentUser();

        // Only agents/admins can post internal notes
        if (request.isInternal() && author.getRole() == User.Role.USER) {
            throw new AccessDeniedException(
                    "Only agents and admins can post internal notes");
        }

        Comment comment = Comment.builder()
                .ticket(ticket)
                .author(author)
                .body(request.getBody())
                .internal(request.isInternal())
                .type(request.getType() != null ? request.getType() : Comment.CommentType.REPLY)
                .build();

        return toResponse(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public CommentResponse addReply(Long parentCommentId, CreateCommentRequest request) {
        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() ->  new ResourceNotFoundException("Comment", parentCommentId));

        // Replies inherit the ticket from the parent
        User author = getCurrentUser();

        Comment reply = Comment.builder()
                .ticket(parent.getTicket())
                .author(author)
                .parent(parent)
                .body(request.getBody())
                .internal(request.isInternal())
                .type(Comment.CommentType.REPLY)
                .build();

        return toResponse(commentRepository.save(reply));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(Long ticketId, Pageable pageable) {
        ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        User current = getCurrentUser();
        boolean isAgent = current.getRole() != User.Role.USER;

        return commentRepository.findByTicketId(ticketId, pageable)
                .map(c -> {
                    // Filter out internal notes for end users
                    if (c.isInternal() && !isAgent) return null;
                    return toResponseWithReplies(c, isAgent);
                })
                .map(r -> r); // keep nulls filtered naturally by Spring Page
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getReplies(Long commentId) {
        commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        return commentRepository.findByParentId(commentId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long commentId, UpdateCommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        User current = getCurrentUser();
        boolean isAdmin = current.getRole() == User.Role.ADMIN;
        boolean isAuthor = comment.getAuthor().getId().equals(current.getId());
        boolean withinWindow = comment.getCreatedAt()
                .isAfter(LocalDateTime.now().minusMinutes(EDIT_WINDOW_MINUTES));

        if (!isAdmin && !(isAuthor && withinWindow)) {
            throw new AccessDeniedException(
                    "Comments can only be edited within " + EDIT_WINDOW_MINUTES
                            + " minutes of creation, or by an admin");
        }

        comment.setBody(request.getBody());
        return toResponse(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        User current = getCurrentUser();
        boolean isAdmin  = current.getRole() == User.Role.ADMIN;
        boolean isAuthor = comment.getAuthor().getId().equals(current.getId());
        boolean withinWindow = comment.getCreatedAt()
                .isAfter(LocalDateTime.now().minusMinutes(EDIT_WINDOW_MINUTES));

        if (!isAdmin && !(isAuthor && withinWindow)) {
            throw new AccessDeniedException(
                    "Comments can only be deleted within " + EDIT_WINDOW_MINUTES
                            + " minutes of creation, or by an admin");
        }

        commentRepository.delete(comment);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private CommentResponse toResponse(Comment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .ticketId(c.getTicket().getId())
                .author(UserResponse.builder()
                        .id(c.getAuthor().getId())
                        .name(c.getAuthor().getName())
                        .email(c.getAuthor().getEmail())
                        .role(c.getAuthor().getRole())
                        .build())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .body(c.getBody())
                .internal(c.isInternal())
                .type(c.getType())
                .createdAt(c.getCreatedAt())
                .replies(List.of())
                .build();
    }

    private CommentResponse toResponseWithReplies(Comment c, boolean includeInternal) {
        List<CommentResponse> replies = commentRepository.findByParentId(c.getId())
                .stream()
                .filter(r -> includeInternal || !r.isInternal())
                .map(this::toResponse)
                .toList();

        CommentResponse response = toResponse(c);
        response.setReplies(replies);
        return response;
    }
}
