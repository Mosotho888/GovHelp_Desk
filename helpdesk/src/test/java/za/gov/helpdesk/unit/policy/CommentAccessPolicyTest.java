package za.gov.helpdesk.unit.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import za.gov.helpdesk.comment.model.Comment;
import za.gov.helpdesk.comment.policy.CommentAccessPolicy;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.users.model.User;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CommentAccessPolicy unit tests")
public class CommentAccessPolicyTest {

    private final CommentAccessPolicy policy = new CommentAccessPolicy();

    private User admin;
    private User agent;
    private User otherAgent;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        admin      = user(1L, "admin@gov.za",  User.Role.ADMIN);
        agent      = user(2L, "agent@gov.za",  User.Role.AGENT);
        otherAgent = user(3L, "other@gov.za",  User.Role.AGENT);

        ticket = Ticket.builder().id(10L).subject("Test").description("desc")
                .status(Ticket.Status.OPEN).requester(agent).build();
    }

    @Test
    @DisplayName("canMutate() returns true for admin regardless of ownership or time window")
    void canMutate_admin_alwaysAllowed() {
        Comment old = comment(admin, LocalDateTime.now().minusHours(5));
        assertThat(policy.canMutate(admin, old)).isTrue();
    }

    @Test
    @DisplayName("canMutate() returns true for author within 15-minute window")
    void canMutate_authorWithinWindow_allowed() {
        Comment fresh = comment(agent, LocalDateTime.now().minusMinutes(10));
        assertThat(policy.canMutate(agent, fresh)).isTrue();
    }

    @Test
    @DisplayName("canMutate() returns false for author outside 15-minute window")
    void canMutate_authorOutsideWindow_denied() {
        Comment old = comment(agent, LocalDateTime.now().minusMinutes(20));
        assertThat(policy.canMutate(agent, old)).isFalse();
    }

    @Test
    @DisplayName("canMutate() returns false for non-author even within window")
    void canMutate_nonAuthorWithinWindow_denied() {
        Comment fresh = comment(agent, LocalDateTime.now().minusMinutes(5));
        assertThat(policy.canMutate(otherAgent, fresh)).isFalse();
    }

    @Test
    @DisplayName("assertCanMutate() throws AccessDeniedException when canMutate() is false")
    void assertCanMutate_denied_throwsAccessDenied() {
        Comment old = comment(agent, LocalDateTime.now().minusMinutes(30));

        assertThatThrownBy(() -> policy.assertCanMutate(otherAgent, old))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("15 minutes");
    }

    @Test
    @DisplayName("assertCanMutate() does not throw when canMutate() is true")
    void assertCanMutate_allowed_doesNotThrow() {
        Comment fresh = comment(admin, LocalDateTime.now().minusHours(10));
        // Should not throw
        policy.assertCanMutate(admin, fresh);
    }

    private User user(Long id, String email, User.Role role) {
        return User.builder().id(id).name("User " + id).email(email).role(role).active(true).build();
    }

    private Comment comment(User author, LocalDateTime createdAt) {
        return Comment.builder()
                .id(100L).ticket(ticket).author(author)
                .body("A comment").internal(false)
                .type(Comment.CommentType.REPLY)
                .createdAt(createdAt).build();
    }
}
