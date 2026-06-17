package za.gov.helpdesk.attachment.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;
import za.gov.helpdesk.users.dto.response.UserResponse;

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
