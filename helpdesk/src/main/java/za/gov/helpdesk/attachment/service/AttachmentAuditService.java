package za.gov.helpdesk.attachment.service;

import za.gov.helpdesk.attachment.model.Attachment;
import za.gov.helpdesk.users.model.User;

public interface AttachmentAuditService {

    void uploadedAttachment(Attachment savedAttachment, User actor, Long ticketId);

    void downloadedAttachment(Attachment attachment, User actor);

    void deleteAttachment(Attachment attachment, User actor);
}
