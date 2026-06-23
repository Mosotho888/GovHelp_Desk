package za.gov.helpdesk.comment.policy;

import java.time.LocalDateTime;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import za.gov.helpdesk.comment.model.Comment;
import za.gov.helpdesk.users.model.User;

/**
 * Access policy component enforcing structural business domain authorization rules for modifying
 * comment data records. Governs operational access permission boundaries based on security role
 * clearings and strict elapsed creation timeline intervals.
 */
@Component
public class CommentAccessPolicy {

    /**
     * The maximum allowable time window interval in minutes during which an author is authorized to
     * mutate or delete their own comment record.
     */
    private static final int EDIT_WINDOW_MINUTES = 15;

    /**
     * Evaluates whether a security actor principal context possesses adequate clearance to mutate
     * or delete a target comment entity instance. Administrators are globally authorized, whereas
     * comment authors are only authorized if the modification attempt occurs within the established
     * editing duration window.
     *
     * @param user the authenticated {@link User} attempting the mutation operation
     * @param comment the existing target {@link Comment} entity to be modified
     * @return true if the access context passes authorization boundaries, false otherwise
     */
    public boolean canMutate(final User user, final Comment comment) {
        final boolean isAdmin = user.getRole() == User.Role.ADMIN;
        final boolean isAuthor = comment.getAuthor().getId().equals(user.getId());
        final boolean withinWindow =
                comment.getCreatedAt()
                        .isAfter(LocalDateTime.now().minusMinutes(EDIT_WINDOW_MINUTES));

        return isAdmin || (isAuthor && withinWindow);
    }

    /**
     * Asserts that a security actor principal context has valid authorization to modify a target
     * comment record. Throws a security boundary exception immediately on failure.
     *
     * @param user the authenticated {@link User} attempting the mutation operation
     * @param comment the existing target {@link Comment} entity to be modified
     * @throws AccessDeniedException if the requesting actor lacks administrator rights or attempts
     *     to edit an owned comment outside the time boundary
     */
    public void assertCanMutate(final User user, final Comment comment) {
        if (!canMutate(user, comment)) {
            throw new AccessDeniedException(
                    "Comments can only be edited/deleted within "
                            + EDIT_WINDOW_MINUTES
                            + " minutes of creation, or by an admin");
        }
    }
}
