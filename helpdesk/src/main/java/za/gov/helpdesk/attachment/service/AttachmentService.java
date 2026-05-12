package za.gov.helpdesk.attachment.service;

import org.springframework.web.multipart.MultipartFile;
import za.gov.helpdesk.attachment.dto.response.AttachmentResponse;

import java.util.List;

public interface AttachmentService {

    List<AttachmentResponse> uploadAttachments(Long ticketId, List<MultipartFile> files);
    List<AttachmentResponse> getAttachments(Long ticketId);
    AttachmentResponse getAttachmentById(Long attachmentId);
    void deleteAttachment(Long attachmentId);

}
