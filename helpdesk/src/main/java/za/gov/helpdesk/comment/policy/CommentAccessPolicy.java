package za.gov.helpdesk.comment.policy;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import za.gov.helpdesk.comment.model.Comment;
import za.gov.helpdesk.users.model.User;

import java.time.LocalDateTime;

@Component
public class CommentAccessPolicy {

    static final int EDIT_WINDOW_MINUTES = 15;

    public boolean canMutate(User user, Comment comment) {
        boolean isAdmin       = user.getRole() == User.Role.ADMIN;
        boolean isAuthor      = comment.getAuthor().getId().equals(user.getId());
        boolean withinWindow  = comment.getCreatedAt()
                .isAfter(LocalDateTime.now().minusMinutes(EDIT_WINDOW_MINUTES));

        return isAdmin || (isAuthor && withinWindow);
    }

    public void assertCanMutate(User user, Comment comment) {
        if (!canMutate(user, comment)) {
            throw new AccessDeniedException(
                    "Comments can only be edited/deleted within "
                            + EDIT_WINDOW_MINUTES + " minutes of creation, or by an admin");
        }
    }
}
