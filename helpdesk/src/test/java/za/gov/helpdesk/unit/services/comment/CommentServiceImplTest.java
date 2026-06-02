package za.gov.helpdesk.unit.services.comment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.comment.dto.request.CreateCommentRequest;
import za.gov.helpdesk.comment.dto.response.CommentResponse;
import za.gov.helpdesk.comment.mapper.CommentMapper;
import za.gov.helpdesk.comment.model.Comment;
import za.gov.helpdesk.comment.repository.CommentRepository;
import za.gov.helpdesk.comment.service.impl.CommentServiceImpl;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService unit tests")
public class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;
    @Mock
    private AuditEventPublisher auditPublisher;

    @InjectMocks
    private CommentServiceImpl commentServiceImpl;

    private User agentUser;
    private User endUser;
    private Ticket ticket;
    private Comment comment;

    @BeforeEach
    void setUp() {
        agentUser = User.builder().id(1L).name("Jane Agent").email("jane@gov.za")
                .role(User.Role.AGENT).active(true).build();
        endUser = User.builder().id(2L).name("John Public").email("john@citizen.za")
                .role(User.Role.USER).active(true).build();

        ticket = Ticket.builder().id(10L).subject("Test ticket")
                .description("desc").status(Ticket.Status.OPEN)
                .requester(endUser).createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).build();

        comment = Comment.builder().id(100L).ticket(ticket).author(agentUser)
                .body("This is a test comment").internal(false)
                .type(Comment.CommentType.REPLY)
                .createdAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("addComment() saves comment and returns response")
    void addComment_valid_savesAndReturns() {

        mockAuthenticatedUser();

        CreateCommentRequest req = new CreateCommentRequest();
        req.setBody("Investigating the issue now.");
        req.setInternal(false);

        given(authentication.getName()).willReturn("jane@gov.za");
        given(userRepository.findByEmail("jane@gov.za")).willReturn(Optional.of(agentUser));
        given(ticketRepository.findById(10L)).willReturn(Optional.of(ticket));
        given(commentRepository.save(any(Comment.class))).willReturn(comment);
        given(commentMapper.toCommentResponse(comment)).willReturn(responseFor(comment));
//        given(commentRepository.findByParentId(any())).willReturn(List.of());

        CommentResponse response = commentServiceImpl.addComment(10L, req, agentUser);

        assertThat(response.getBody()).isEqualTo("This is a test comment");
        assertThat(response.isInternal()).isFalse();
        then(commentRepository).should(times(1)).save(any(Comment.class));
    }

    @Test
    @DisplayName("addComment() blocks end users from posting internal notes")
    void addComment_internalByUser_throwsAccessDenied() {

        mockAuthenticatedUser();

        CreateCommentRequest req = new CreateCommentRequest();
        req.setBody("Internal note.");
        req.setInternal(true);

        given(authentication.getName()).willReturn("john@citizen.za");
        given(userRepository.findByEmail("john@citizen.za")).willReturn(Optional.of(endUser));
        given(ticketRepository.findById(10L)).willReturn(Optional.of(ticket));

        assertThatThrownBy(() -> commentServiceImpl.addComment(10L, req, endUser))
                .isInstanceOf(AccessDeniedException.class);

        then(commentRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("addComment() throws ResourceNotFoundException for unknown ticket")
    void addComment_unknownTicket_throwsNotFound() {

        CreateCommentRequest req = new CreateCommentRequest();
        req.setBody("Test");

        given(ticketRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentServiceImpl.addComment(999L, req, endUser))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteComment() blocks deletion outside 15-minute window by non-admin")
    void deleteComment_outsideWindow_throwsAccessDenied() {
        mockAuthenticatedUser();
        Comment oldComment = Comment.builder().id(200L).ticket(ticket).author(endUser)
                .body("Old comment").internal(false).type(Comment.CommentType.REPLY)
                .createdAt(LocalDateTime.now().minusMinutes(20))
                .build();

        given(authentication.getName()).willReturn("john@citizen.za");
        given(userRepository.findByEmail("john@citizen.za")).willReturn(Optional.of(endUser));
        given(commentRepository.findById(200L)).willReturn(Optional.of(oldComment));

        assertThatThrownBy(() -> commentServiceImpl.deleteComment(200L, endUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("15 minutes");
    }

    @Test
    @DisplayName("addReply() creates reply linked to parent comment")
    void addReply_valid_linksToParent() {
        mockAuthenticatedUser();
        CreateCommentRequest req = new CreateCommentRequest();
        req.setBody("Thanks for the update!");

        Comment replyComment = Comment.builder().id(101L).ticket(ticket).author(endUser)
                .parent(comment).body("Thanks for the update!").internal(false)
                .type(Comment.CommentType.REPLY).createdAt(LocalDateTime.now()).build();

        given(authentication.getName()).willReturn("john@citizen.za");
        given(userRepository.findByEmail("john@citizen.za")).willReturn(Optional.of(endUser));
        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));
        given(commentRepository.save(any(Comment.class))).willReturn(replyComment);
        given(commentMapper.toCommentResponse(replyComment)).willReturn(responseFor(replyComment));

        CommentResponse response = commentServiceImpl.addReply(100L, req, endUser);

        assertThat(response.getParentId()).isEqualTo(100L);
        assertThat(response.getBody()).isEqualTo("Thanks for the update!");
        assertThat(response.getType()).isEqualTo(Comment.CommentType.REPLY);

        verify(commentRepository, times(1)).save(argThat(savedReply ->
                savedReply.getParent().equals(comment)
                        && savedReply.getTicket().equals(ticket)
                        && savedReply.getAuthor().equals(endUser)
                        && savedReply.getBody().equals("Thanks for the update!")
        ));

        then(auditPublisher).should(times(1)).publishAudit(
                eq(AuditLog.EntityType.COMMENT),
                eq(replyComment.getId()),
                eq(endUser),
                eq(AuditLog.AuditAction.COMMENT_ADDED),
                isNull(),
                eq(String.valueOf(replyComment.getId())),
                any(String.class)
        );
    }

    private void mockAuthenticatedUser() {
        SecurityContextHolder.setContext(securityContext);
        given(securityContext.getAuthentication()).willReturn(authentication);

    }

    private CommentResponse responseFor(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .ticketId(comment.getTicket().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .body(comment.getBody())
                .internal(comment.isInternal())
                .type(comment.getType())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
