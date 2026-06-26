package za.gov.helpdesk.comment.service.impl;

import java.util.List;

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
import za.gov.helpdesk.comment.service.CommentQueryHelper;
import za.gov.helpdesk.comment.service.CommentService;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.service.TicketQueryHelper;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final int MIN_BODY = 0;
    private static final int MAX_BODY = 80;

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final TicketQueryHelper ticketQuery;
    private final CommentQueryHelper commentQuery;
    private final AuditEventPublisher auditPublisher;
    private final CommentAccessPolicy accessPolicy;
    private final CommentMetrics commentMetrics;

    @Override
    @Transactional
    public CommentResponse addComment(
            final Long ticketId, final CreateCommentRequest request, final User actor) {
        final Ticket ticket = ticketQuery.findOrThrow(ticketId, actor);

        if (request.isInternal() && actor.getRole() == User.Role.USER) {
            throw new AccessDeniedException("Only agents and admins can post internal notes");
        }

        final Comment comment =
                Comment.builder()
                        .ticket(ticket)
                        .author(actor)
                        .body(request.getBody())
                        .internal(request.isInternal())
                        .type(
                                request.getType() != null
                                        ? request.getType()
                                        : Comment.CommentType.REPLY)
                        .build();

        final Comment savedComment = commentRepository.save(comment);

        if (request.isInternal()) {
            commentMetrics.incrementInternalNoteAdded();
        } else {
            commentMetrics.incrementAdded();
        }

        final AuditLog.AuditAction action =
                request.isInternal()
                        ? AuditLog.AuditAction.INTERNAL_NOTE_ADDED
                        : AuditLog.AuditAction.COMMENT_ADDED;

        auditPublisher.publishAudit(
                AuditLog.EntityType.COMMENT,
                savedComment.getId(),
                actor,
                action,
                null,
                String.valueOf(savedComment.getId()),
                (request.isInternal() ? "Internal note" : "Comment") + " on ticket #" + ticketId);

        return commentMapper.toCommentResponse(savedComment);
    }

    @Override
    @Transactional
    public CommentResponse addReply(
            final Long parentCommentId, final CreateCommentRequest request, final User actor) {
        final Comment parent = commentQuery.findOrThrow(parentCommentId, actor);

        if (request.isInternal() && actor.getRole() == User.Role.USER) {
            throw new AccessDeniedException("Only agents and admins can post internal notes");
        }

        final Comment reply =
                Comment.builder()
                        .ticket(parent.getTicket())
                        .author(actor)
                        .parent(parent)
                        .body(request.getBody())
                        .internal(request.isInternal())
                        .type(Comment.CommentType.REPLY)
                        .build();

        final Comment savedReply = commentRepository.save(reply);

        commentMetrics.incrementAdded();

        auditPublisher.publishAudit(
                AuditLog.EntityType.COMMENT,
                savedReply.getId(),
                actor,
                AuditLog.AuditAction.COMMENT_ADDED,
                null,
                String.valueOf(savedReply.getId()),
                "Reply to comment #"
                        + parentCommentId
                        + " on ticket #"
                        + parent.getTicket().getId());

        return commentMapper.toCommentResponse(savedReply);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(
            final Long ticketId, final Pageable pageable, final User actor) {
        ticketQuery.findOrThrow(ticketId, actor);

        return commentQuery
                .findVisibleCommentsSecurely(ticketId, actor, pageable)
                .map(c -> toResponseWithReplies(c, actor));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getReplies(final Long commentId, final User actor) {
        commentQuery.findOrThrow(commentId, actor);

        return commentQuery.findVisibleReplies(commentId, actor).stream()
                .map(commentMapper::toCommentResponse)
                .toList();
    }

    @Override
    @Transactional
    public CommentResponse updateComment(
            final Long commentId, final UpdateCommentRequest request, final User actor) {
        final Comment comment = commentQuery.findOrThrow(commentId, actor);

        accessPolicy.assertCanMutate(actor, comment);

        final String oldBody = comment.getBody();
        comment.setBody(request.getBody());
        final Comment savedComment = commentRepository.save(comment);

        commentMetrics.incrementEdited();

        auditPublisher.publishAudit(
                AuditLog.EntityType.COMMENT,
                savedComment.getId(),
                actor,
                AuditLog.AuditAction.COMMENT_EDITED,
                oldBody.length() > MAX_BODY ? oldBody.substring(MIN_BODY, MAX_BODY) + "…" : oldBody,
                null,
                "Comment edited on ticket #" + comment.getTicket().getId());
        return commentMapper.toCommentResponse(savedComment);
    }

    @Override
    @Transactional
    public void deleteComment(final Long commentId, final User actor) {
        final Comment comment = commentQuery.findOrThrow(commentId, actor);

        accessPolicy.assertCanMutate(actor, comment);

        auditPublisher.publishAudit(
                AuditLog.EntityType.COMMENT,
                comment.getId(),
                actor,
                AuditLog.AuditAction.COMMENT_DELETED,
                String.valueOf(comment.getId()),
                null,
                "Comment deleted from ticket #" + comment.getTicket().getId());

        commentRepository.delete(comment);
        commentMetrics.incrementDeleted();
    }

    private CommentResponse toResponseWithReplies(final Comment comment, final User actor) {
        final List<CommentResponse> replies =
                commentQuery.findVisibleReplies(comment.getId(), actor).stream()
                        .map(commentMapper::toCommentResponse)
                        .toList();

        final CommentResponse response = commentMapper.toCommentResponse(comment);
        response.setReplies(replies);
        return response;
    }
}
