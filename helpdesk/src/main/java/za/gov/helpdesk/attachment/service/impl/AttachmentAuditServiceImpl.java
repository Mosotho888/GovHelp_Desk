package za.gov.helpdesk.attachment.service.impl;

import org.springframework.stereotype.Service;

import za.gov.helpdesk.attachment.model.Attachment;
import za.gov.helpdesk.attachment.service.AttachmentAuditService;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttachmentAuditServiceImpl implements AttachmentAuditService {

    public static final long KB = 1024L;

    private final AuditEventPublisher auditPublisher;

    @Override
    public void uploadedAttachment(
            final Attachment savedAttachment, final User actor, final Long ticketId) {

        auditPublisher.publishAudit(
                AuditLog.EntityType.ATTACHMENT,
                savedAttachment.getId(),
                actor,
                AuditLog.AuditAction.ATTACHMENT_UPLOADED,
                null,
                savedAttachment.getFilename(),
                "Uploaded to ticket #"
                        + ticketId
                        + " ("
                        + (savedAttachment.getSizeBytes() / KB)
                        + " KB)");
    }

    @Override
    public void downloadedAttachment(final Attachment attachment, final User actor) {

        auditPublisher.publishAudit(
                AuditLog.EntityType.ATTACHMENT,
                attachment.getId(),
                actor,
                AuditLog.AuditAction.ATTACHMENT_DOWNLOADED,
                null,
                attachment.getFilename(),
                "Downloaded to ticket #" + attachment.getTicket().getId());
    }

    @Override
    public void deleteAttachment(final Attachment attachment, final User actor) {

        auditPublisher.publishAudit(
                AuditLog.EntityType.ATTACHMENT,
                attachment.getId(),
                actor,
                AuditLog.AuditAction.ATTACHMENT_DELETED,
                attachment.getFilename(),
                null,
                "Deleted from ticket #" + attachment.getTicket().getId());
    }
}
