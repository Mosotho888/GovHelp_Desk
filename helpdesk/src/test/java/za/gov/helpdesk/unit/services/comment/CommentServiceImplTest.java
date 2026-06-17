package za.gov.helpdesk.unit.services.comment;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import za.gov.helpdesk.agent.model.Agent;
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
import za.gov.helpdesk.comment.service.impl.CommentServiceImpl;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.users.model.User;

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
    private AuditEventPublisher auditPublisher;
    @Mock
    private CommentAccessPolicy accessPolicy;
    @Mock
    private CommentMetrics commentMetrics;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User agentUser;
    private Agent agent;
    private User endUser;
    private Ticket ticket;
    private Comment comment;

    @BeforeEach
    void setUp() {
        agentUser = User.builder().id(1L).name("Jane Agent").email("jane@gov.za").role(User.Role.AGENT).active(true)
                .build();
        agent = Agent.builder().id(1L).user(agentUser).department("Tech").availability(Agent.Availability.ONLINE)
                .build();
        endUser = User.builder().id(2L).name("John Public").email("john@citizen.za").role(User.Role.USER).active(true)
                .build();

        ticket = Ticket.builder().id(10L).subject("Test ticket").description("desc").status(Ticket.Status.OPEN)
                .requester(endUser).assignee(agent).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        comment = Comment.builder().id(100L).ticket(ticket).author(agentUser).body("This is a test comment")
                .internal(false).type(Comment.CommentType.REPLY).createdAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("addComment() saves comment, publishes COMMENT_ADDED audit, and returns response")
    void addComment_valid_savesAndPublishesAudit() {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setBody("Investigating the issue.");
        req.setInternal(false);

        given(ticketRepository.findById(10L)).willReturn(Optional.of(ticket));
        given(commentRepository.save(any(Comment.class))).willReturn(comment);
        given(commentMapper.toCommentResponse(comment)).willReturn(responseFor(comment));

        CommentResponse response = commentService.addComment(10L, req, agentUser);

        then(commentMetrics).should(times(1)).incrementAdded();
        then(commentMetrics).should(never()).incrementInternalNoteAdded();

        assertThat(response.getBody()).isEqualTo("This is a test comment");
        assertThat(response.isInternal()).isFalse();
        then(commentRepository).should(times(1)).save(any(Comment.class));
        then(auditPublisher).should(times(1)).publishAudit(eq(AuditLog.EntityType.COMMENT), eq(comment.getId()),
                eq(agentUser), eq(AuditLog.AuditAction.COMMENT_ADDED), isNull(), any(), any());
    }

    @Test
    @DisplayName("addComment() saves internal note and publishes INTERNAL_NOTE_ADDED audit")
    void addComment_internalByAgent_savesAndPublishesInternalNoteAudit() {

        CreateCommentRequest req = new CreateCommentRequest();
        req.setBody("Internal: checking database");
        req.setInternal(true);

        given(ticketRepository.findById(10L)).willReturn(Optional.of(ticket));

        Comment internalNote = Comment.builder().id(101L).ticket(ticket).author(agentUser)
                .body("Internal: checking database").internal(true).type(Comment.CommentType.NOTE)
                .createdAt(LocalDateTime.now()).build();

        given(commentRepository.save(any(Comment.class))).willReturn(internalNote);
        given(commentMapper.toCommentResponse(internalNote)).willReturn(responseFor(internalNote));

        CommentResponse response = commentService.addComment(10L, req, agentUser);

        then(commentMetrics).should(times(1)).incrementInternalNoteAdded();
        then(commentMetrics).should(never()).incrementAdded();

        assertThat(response.isInternal()).isTrue();
        then(auditPublisher).should(times(1)).publishAudit(eq(AuditLog.EntityType.COMMENT), eq(internalNote.getId()),
                eq(agentUser), eq(AuditLog.AuditAction.INTERNAL_NOTE_ADDED), isNull(), any(), any());
    }

    @Test
    @DisplayName("addComment() throws AccessDeniedException when end user posts internal note")
    void addComment_internalByUser_throwsAccessDenied() {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setBody("Internal note.");
        req.setInternal(true);

        given(ticketRepository.findById(10L)).willReturn(Optional.of(ticket));

        assertThatThrownBy(() -> commentService.addComment(10L, req, endUser))
                .isInstanceOf(AccessDeniedException.class);

        then(commentRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("addComment() throws ResourceNotFoundException for unknown ticket")
    void addComment_unknownTicket_throwsNotFound() {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setBody("Test");

        given(ticketRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(999L, req, endUser))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("999");
    }

    @Test
    @DisplayName("addReply() creates reply linked to parent comment")
    void addReply_valid_linksToParent() {
        Comment replyComment = Comment.builder().id(101L).ticket(ticket).author(endUser).parent(comment)
                .body("Thanks for the update!").internal(false).type(Comment.CommentType.REPLY)
                .createdAt(LocalDateTime.now()).build();

        CreateCommentRequest req = new CreateCommentRequest();
        req.setBody("Thanks for the update!");

        given(commentRepository.findByIdForActor(100L, endUser.getEmail(), endUser.getRole().name()))
                .willReturn(Optional.of(comment));
        given(commentRepository.save(any(Comment.class))).willReturn(replyComment);
        given(commentMapper.toCommentResponse(replyComment)).willReturn(responseFor(replyComment));

        CommentResponse response = commentService.addReply(100L, req, endUser);

        assertThat(response.getParentId()).isEqualTo(100L);
        assertThat(response.getBody()).isEqualTo("Thanks for the update!");
        verify(commentRepository, times(1))
                .save(argThat(saved -> saved.getParent().equals(comment) && saved.getTicket().equals(ticket)
                        && saved.getAuthor().equals(endUser) && saved.getBody().equals("Thanks for the update!")));
        then(auditPublisher).should(times(1)).publishAudit(eq(AuditLog.EntityType.COMMENT), eq(replyComment.getId()),
                eq(endUser), eq(AuditLog.AuditAction.COMMENT_ADDED), isNull(), any(), any());
    }

    @Test
    @DisplayName("addReply() throws ResourceNotFoundException for unknown parent comment")
    void addReply_unknownParent_throwsNotFound() {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setBody("Reply");

        given(commentRepository.findByIdForActor(999L, agentUser.getEmail(), agentUser.getRole().name()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addReply(999L, req, agentUser))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("999");
    }

    @Test
    @DisplayName("updateComment() updates body, publishes COMMENT_EDITED audit")
    void updateComment_valid_updatesAndPublishesAudit() {
        UpdateCommentRequest req = new UpdateCommentRequest();
        req.setBody("Corrected body");

        given(commentRepository.findByIdForActor(100L, agentUser.getEmail(), agentUser.getRole().name()))
                .willReturn(Optional.of(comment));
        given(commentRepository.save(any(Comment.class))).willReturn(comment);
        given(commentMapper.toCommentResponse(comment)).willReturn(responseFor(comment));
        // accessPolicy.canMutate returns true (default Mockito behaviour — void, no throw)

        commentService.updateComment(100L, req, agentUser);

        then(commentMetrics).should(times(1)).incrementEdited();

        then(auditPublisher).should(times(1)).publishAudit(eq(AuditLog.EntityType.COMMENT), eq(comment.getId()),
                eq(agentUser), eq(AuditLog.AuditAction.COMMENT_EDITED), any(), isNull(), any());
    }

    @Test
    @DisplayName("updateComment() throws AccessDeniedException when policy rejects mutation")
    void updateComment_policyDenied_throwsAccessDenied() {
        UpdateCommentRequest req = new UpdateCommentRequest();
        req.setBody("Sneaky edit");

        given(commentRepository.findByIdForActor(100L, endUser.getEmail(), endUser.getRole().name()))
                .willReturn(Optional.of(comment));
        willThrow(new AccessDeniedException("Comments can only be edited/deleted within 15 minutes"))
                .given(accessPolicy).assertCanMutate(endUser, comment);

        assertThatThrownBy(() -> commentService.updateComment(100L, req, endUser))
                .isInstanceOf(AccessDeniedException.class).hasMessageContaining("15 minutes");

        then(commentRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("deleteComment() publishes COMMENT_DELETED audit and removes from repository")
    void deleteComment_valid_publishesAuditAndDeletes() {
        given(commentRepository.findById(100L)).willReturn(Optional.of(comment));

        commentService.deleteComment(100L, agentUser);

        then(auditPublisher).should(times(1)).publishAudit(eq(AuditLog.EntityType.COMMENT), eq(comment.getId()),
                eq(agentUser), eq(AuditLog.AuditAction.COMMENT_DELETED), any(), isNull(), any());
        then(commentMetrics).should(times(1)).incrementDeleted();
        then(commentRepository).should(times(1)).delete(comment);
    }

    @Test
    @DisplayName("deleteComment() throws AccessDeniedException when policy rejects deletion")
    void deleteComment_policyDenied_throwsAccessDenied() {
        Comment oldComment = Comment.builder().id(200L).ticket(ticket).author(endUser).body("Old comment")
                .internal(false).type(Comment.CommentType.REPLY).createdAt(LocalDateTime.now().minusMinutes(20))
                .build();

        given(commentRepository.findById(200L)).willReturn(Optional.of(oldComment));
        willThrow(new AccessDeniedException("Comments can only be edited/deleted within 15 minutes"))
                .given(accessPolicy).assertCanMutate(endUser, oldComment);

        assertThatThrownBy(() -> commentService.deleteComment(200L, endUser)).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("15 minutes");

        then(commentRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("deleteComment() throws ResourceNotFoundException for unknown comment")
    void deleteComment_unknownComment_throwsNotFound() {
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(999L, agentUser))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private CommentResponse responseFor(Comment c) {
        return CommentResponse.builder().id(c.getId()).ticketId(c.getTicket().getId())
                .parentId(c.getParent() != null ? c.getParent().getId() : null).body(c.getBody())
                .internal(c.isInternal()).type(c.getType()).createdAt(c.getCreatedAt()).build();
    }
}
