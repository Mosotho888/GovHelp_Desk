package za.gov.helpdesk.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.gov.helpdesk.comment.dto.CommentResponse;
import za.gov.helpdesk.comment.dto.CreateCommentRequest;
import za.gov.helpdesk.comment.dto.UpdateCommentRequest;
import za.gov.helpdesk.comment.service.CommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Ticket comments and replies")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/tickets/{ticketId}/comments")
    @Operation(summary = "Add a comment to a ticket")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long ticketId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(ticketId, request));
    }

    @GetMapping("/tickets/{ticketId}/comments")
    @Operation(summary = "List comments on a ticket (internal notes hidden for end users)")
    public ResponseEntity<Page<CommentResponse>> getComments(
            @PathVariable Long ticketId,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(commentService.getComments(ticketId, pageable));
    }

    @PostMapping("/comments/{commentId}/replies")
    @Operation(summary = "Reply to an existing comment")
    public ResponseEntity<CommentResponse> addReply(
            @PathVariable Long commentId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addReply(commentId, request));
    }

    @GetMapping("/comments/{commentId}/replies")
    @Operation(summary = "Get all replies to a comment")
    public ResponseEntity<List<CommentResponse>> getReplies(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.getReplies(commentId));
    }

    @PutMapping("/v1/comments/{commentId}")
    @Operation(summary = "Edit a comment (within 15 minutes of creation, or admin)")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        return ResponseEntity.ok(commentService.updateComment(commentId, request));
    }

    @DeleteMapping("/v1/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a comment (within 15 minutes of creation, or admin)")
    public void deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
    }
}
