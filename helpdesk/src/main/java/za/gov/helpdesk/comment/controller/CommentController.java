package za.gov.helpdesk.comment.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import za.gov.helpdesk.comment.dto.request.CreateCommentRequest;
import za.gov.helpdesk.comment.dto.request.UpdateCommentRequest;
import za.gov.helpdesk.comment.dto.response.CommentResponse;
import za.gov.helpdesk.comment.service.CommentService;
import za.gov.helpdesk.users.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Ticket comments and replies")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/v1")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/tickets/{ticketId}/comments")
    @Operation(summary = "Add a comment to a ticket")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable final Long ticketId,
            @Valid @RequestBody final CreateCommentRequest request,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(ticketId, request, principal.getUser()));
    }

    @GetMapping("/tickets/{ticketId}/comments")
    @Operation(summary = "List comments on a ticket (internal notes hidden for end users)")
    public ResponseEntity<Page<CommentResponse>> getComments(
            @PathVariable final Long ticketId,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.ASC)
                    final Pageable pageable,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.ok(
                commentService.getComments(ticketId, pageable, principal.getUser()));
    }

    @PostMapping("/comments/{commentId}/replies")
    @Operation(summary = "Reply to an existing comment")
    public ResponseEntity<CommentResponse> addReply(
            @PathVariable final Long commentId,
            @Valid @RequestBody final CreateCommentRequest request,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addReply(commentId, request, principal.getUser()));
    }

    @GetMapping("/comments/{commentId}/replies")
    @Operation(summary = "Get all replies to a comment")
    public ResponseEntity<List<CommentResponse>> getReplies(
            @PathVariable final Long commentId,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.ok(commentService.getReplies(commentId, principal.getUser()));
    }

    @PutMapping("/comments/{commentId}")
    @Operation(summary = "Edit a comment (within 15 minutes of creation, or admin)")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable final Long commentId,
            @Valid @RequestBody final UpdateCommentRequest request,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.ok(
                commentService.updateComment(commentId, request, principal.getUser()));
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a comment (within 15 minutes of creation, or admin)")
    public void deleteComment(
            @PathVariable final Long commentId,
            @AuthenticationPrincipal final CustomUserDetails principal) {

        commentService.deleteComment(commentId, principal.getUser());
    }
}
