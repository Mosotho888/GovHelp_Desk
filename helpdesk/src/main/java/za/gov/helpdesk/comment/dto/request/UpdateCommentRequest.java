package za.gov.helpdesk.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class UpdateCommentRequest {

    @NotBlank(message = "Comment body is required")
    @Size(max = 10000, message = "Comment must not exceed 10,000 characters")
    private String body;
}
