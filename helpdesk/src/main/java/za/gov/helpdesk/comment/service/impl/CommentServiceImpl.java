package za.gov.helpdesk.comment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.comment.dto.CommentResponse;
import za.gov.helpdesk.comment.dto.CreateCommentRequest;
import za.gov.helpdesk.comment.model.Comment;
import za.gov.helpdesk.comment.repository.CommentRepository;
import za.gov.helpdesk.comment.service.CommentService;
import za.gov.helpdesk.ticket.exception.TicketNotFoundException;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.TicketRepository;
import za.gov.helpdesk.users.dto.UserResponse;
import za.gov.helpdesk.users.exception.UserNotFoundException;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

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
                .orElseThrow(TicketNotFoundException::new);
        User author = getCurrentUser();

        // Only agents/admins can post internal notes
        if (request.isInternal() && author.getRole() == User.Role.USER) {
            throw new org.springframework.security.access.AccessDeniedException(
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
                .orElseThrow(TicketNotFoundException::new);

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
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getReplies(Long commentId) {
        return List.of();
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long commentId, CreateCommentRequest request) {
        return null;
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {

    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException());
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
