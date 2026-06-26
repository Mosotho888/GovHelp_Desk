package za.gov.helpdesk.comment.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.comment.model.Comment;
import za.gov.helpdesk.comment.repository.CommentRepository;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;

/**
 * Utility query component providing secure lookup capabilities for ticket comments and replies.
 * Coordinates data visibility filtering by intercepting database access layers and evaluating the
 * requesting user's identity credentials and security role attributes.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryHelper {

    private final CommentRepository commentRepository;

    /**
     * Locates a comment record by its unique identifier, filtering output dynamically based on
     * whether the requesting actor context possesses clear eligibility to view it.
     *
     * @param commentId the unique identifier of the target comment
     * @param actor the authenticated {@link User} performing the data search operation
     * @return the located {@link Comment} entity record
     * @throws ResourceNotFoundException if the comment does not exist or is invisible to the user
     *     context
     */
    public Comment findOrThrow(final Long commentId, final User actor) {
        return commentRepository
                .findByIdForActor(commentId, actor.getEmail(), actor.getRole().name())
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
    }

    /**
     * Resolves a paginated collection of top-level comments structurally bound to a ticket,
     * suppressing internal or restricted items if the security actor lacks adequate role clearing.
     *
     * @param ticketId the unique container ticket identifier tracking the commentary thread
     * @param actor the authenticated {@link User} executing the paginated query loop
     * @param pageable pagination and sorting configuration directives
     * @return a paginated {@link Page} container holding matching visible {@link Comment} nodes
     */
    public Page<Comment> findVisibleCommentsSecurely(
            final Long ticketId, final User actor, final Pageable pageable) {
        // Optimised to fetch root items; sub-elements are pulled or handled cleanly
        return commentRepository.findVisibleByTicketId(
                ticketId, actor.getEmail(), actor.getRole().name(), pageable);
    }

    /**
     * Extracts a list of nested sub-replies mapped hierarchically to a parent comment node,
     * applying visibility rules based on the user's role authority.
     *
     * @param commentId the unique identifier of the parent comment container
     * @param actor the authenticated {@link User} pulling the replies thread
     * @return a {@link List} containing visible child {@link Comment} replies
     */
    public List<Comment> findVisibleReplies(final Long commentId, final User actor) {
        return commentRepository.findVisibleReplies(commentId, actor.getRole().name());
    }
}
