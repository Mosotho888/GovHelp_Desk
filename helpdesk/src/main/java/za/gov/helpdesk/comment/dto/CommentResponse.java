package za.gov.helpdesk.comment.dto;


import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long commenterId,
        String commenterEmail,
        String role,
        String comment,
        LocalDateTime created_at
) { }
