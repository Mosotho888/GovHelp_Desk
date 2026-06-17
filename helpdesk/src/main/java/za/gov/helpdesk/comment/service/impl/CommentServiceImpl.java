package za.gov.helpdesk.comment.service.impl;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.comment.dto.request.CreateCommentRequest;
import za.gov.helpdesk.comment.dto.request.UpdateCommentRequest;
import za.gov.helpdesk.comment.dto.response.CommentResponse;
import za.gov.helpdesk.comment.mapper.CommentMapper;
import za.gov.helpdesk.comment.metrics.CommentMetrics;
import za.gov.helpdesk.comment.model.Comment;
import za.gov.helpdesk.comment.policy.CommentAccessPolicy;
import za.gov.helpdesk.comment.repository.CommentRepository;
import za.gov.helpdesk.comment.service.CommentService;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.users.model.User;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final TicketRepository ticketRepository;
    private final AuditEventPublisher auditPublisher;
    private final CommentAccessPolicy accessPolicy;
    private final CommentMetrics commentMetrics;

    private static final int MIN_BODY = 0;
    private static final int MAX_BODY = 80;

    @Override
    @Transactional
    public CommentResponse addComment(Long ticketId, CreateCommentRequest request, User actor) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        validateCommentAccess(ticket, actor);

        if (request.isInternal() && actor.getRole() == User.Role.USER) {
            throw new AccessDeniedException("Only agents and admins can post internal notes");
        }

        Comment comment = Comment.builder().ticket(ticket).author(actor).body(request.getBody())
                .internal(request.isInternal())
                .type(request.getType() != null ? request.getType() : Comment.CommentType.REPLY).build();

        Comment savedComment = commentRepository.save(comment);

        if (request.isInternal()) {
            commentMetrics.incrementInternalNoteAdded();
        } else {
            commentMetrics.incrementAdded();
        }

        AuditLog.AuditAction action = request.isInternal()
                ? AuditLog.AuditAction.INTERNAL_NOTE_ADDED
                : AuditLog.AuditAction.COMMENT_ADDED;

        auditPublisher.publishAudit(AuditLog.EntityType.COMMENT, savedComment.getId(), actor, action, null,
                String.valueOf(savedComment.getId()),
                (request.isInternal() ? "Internal note" : "Comment") + " on ticket #" + ticketId);

        return commentMapper.toCommentResponse(savedComment);
    }

    @Override
    @Transactional
    public CommentResponse addReply(Long parentCommentId, CreateCommentRequest request, User actor) {
        Comment parent = commentRepository.findByIdForActor(parentCommentId, actor.getEmail(), actor.getRole().name())
                .orElseThrow(() -> new ResourceNotFoundException("Comment", parentCommentId));

        if (request.isInternal() && actor.getRole() == User.Role.USER) {
            throw new AccessDeniedException("Only agents and admins can post internal notes");
        }

        Comment reply = Comment.builder().ticket(parent.getTicket()).author(actor).parent(parent)
                .body(request.getBody()).internal(request.isInternal()).type(Comment.CommentType.REPLY).build();

        Comment savedReply = commentRepository.save(reply);

        commentMetrics.incrementAdded();

        auditPublisher.publishAudit(AuditLog.EntityType.COMMENT, savedReply.getId(), actor,
                AuditLog.AuditAction.COMMENT_ADDED, null, String.valueOf(savedReply.getId()),
                "Reply to comment #" + parentCommentId + " on ticket #" + parent.getTicket().getId());

        return commentMapper.toCommentResponse(savedReply);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(Long ticketId, Pageable pageable, User actor) {
        ticketRepository.findById(ticketId).orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        return commentRepository.findVisibleByTicketId(ticketId, actor.getEmail(), actor.getRole().name(), pageable)
                .map(c -> toResponseWithReplies(c, actor));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getReplies(Long commentId, User actor) {
        commentRepository.findByIdForActor(commentId, actor.getEmail(), actor.getRole().name())
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        return commentRepository.findVisibleReplies(commentId, actor.getRole().name()).stream()
                .map(commentMapper::toCommentResponse).toList();
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long commentId, UpdateCommentRequest request, User actor) {
        Comment comment = commentRepository.findByIdForActor(commentId, actor.getEmail(), actor.getRole().name())
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        accessPolicy.assertCanMutate(actor, comment);

        String oldBody = comment.getBody();
        comment.setBody(request.getBody());
        Comment savedComment = commentRepository.save(comment);

        commentMetrics.incrementEdited();

        auditPublisher.publishAudit(AuditLog.EntityType.COMMENT, savedComment.getId(), actor,
                AuditLog.AuditAction.COMMENT_EDITED,
                oldBody.length() > MAX_BODY ? oldBody.substring(MIN_BODY, MAX_BODY) + "…" : oldBody, null,
                "Comment edited on ticket #" + comment.getTicket().getId());
        return commentMapper.toCommentResponse(savedComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, User actor) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        accessPolicy.assertCanMutate(actor, comment);

        auditPublisher.publishAudit(AuditLog.EntityType.COMMENT, comment.getId(), actor,
                AuditLog.AuditAction.COMMENT_DELETED, String.valueOf(comment.getId()), null,
                "Comment deleted from ticket #" + comment.getTicket().getId());

        commentRepository.delete(comment);
        commentMetrics.incrementDeleted();
    }

    private CommentResponse toResponseWithReplies(Comment comment, User actor) {
        List<CommentResponse> replies = commentRepository.findVisibleReplies(comment.getId(), actor.getRole().name())
                .stream().map(commentMapper::toCommentResponse).toList();

        CommentResponse response = commentMapper.toCommentResponse(comment);
        response.setReplies(replies);
        return response;
    }

    private void validateCommentAccess(Ticket ticket, User actor) {

        if (actor.getRole() == User.Role.ADMIN) {
            return;
        }

        if (actor.getRole() == User.Role.USER && ticket.getRequester().getId().equals(actor.getId())) {
            return;
        }

        if (actor.getRole() == User.Role.AGENT && ticket.getAssignee() != null
                && ticket.getAssignee().getUser().getId().equals(actor.getId())) {
            return;
        }

        throw new AccessDeniedException("You are not allowed to comment on this ticket");
    }
}
