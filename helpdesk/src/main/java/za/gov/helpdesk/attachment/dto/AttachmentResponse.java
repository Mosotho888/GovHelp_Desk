package za.gov.helpdesk.attachment.dto;

import lombok.Builder;
import lombok.Data;
import za.gov.helpdesk.users.dto.UserResponse;

import java.time.LocalDateTime;

@Data
@Builder
public class AttachmentResponse {
    private Long id;
    private Long ticketId;
    private UserResponse uploader;
    private String filename;
    private String contentType;
    private Long sizeBytes;
    private String downloadUrl;
    private LocalDateTime createdAt;
}
