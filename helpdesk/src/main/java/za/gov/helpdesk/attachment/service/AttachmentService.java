package za.gov.helpdesk.attachment.service;

import org.springframework.web.multipart.MultipartFile;
import za.gov.helpdesk.attachment.dto.response.AttachmentResponse;
import za.gov.helpdesk.attachment.model.Attachment;
import za.gov.helpdesk.users.model.User;

import java.util.List;

public interface AttachmentService {

    List<AttachmentResponse> uploadAttachments(Long ticketId, List<MultipartFile> files, User agent);
    List<AttachmentResponse> getAttachments(Long ticketId, User agent);
    Attachment getAttachmentById(Long attachmentId, User agent);
    void deleteAttachment(Long attachmentId,  User agent);

}
