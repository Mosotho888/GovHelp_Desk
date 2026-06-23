package za.gov.helpdesk.attachment.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import za.gov.helpdesk.attachment.dto.response.AttachmentResponse;
import za.gov.helpdesk.attachment.mapper.AttachmentMapper;
import za.gov.helpdesk.attachment.metrics.AttachmentMetrics;
import za.gov.helpdesk.attachment.model.Attachment;
import za.gov.helpdesk.attachment.policy.AttachmentValidator;
import za.gov.helpdesk.attachment.repository.AttachmentRepository;
import za.gov.helpdesk.attachment.service.AttachmentQueryHelper;
import za.gov.helpdesk.attachment.service.AttachmentService;
import za.gov.helpdesk.attachment.service.storage.FileStorageService;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.service.TicketQueryHelper;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentServiceImpl implements AttachmentService {

    public static final long KB = 1024L;

    private final AttachmentRepository attachmentRepository;
    private final AttachmentMapper attachmentMapper;
    private final TicketQueryHelper ticketQuery;
    private final AttachmentQueryHelper attachmentQuery;
    private final AuditEventPublisher auditPublisher;
    private final FileStorageService fileStorageService;
    private final AttachmentValidator validator;
    private final AttachmentMetrics attachmentMetrics;

    @Override
    @Transactional
    public List<AttachmentResponse> uploadAttachments(
            final Long ticketId, final List<MultipartFile> files, final User actor) {

        validator.validateBatch(files);

        final Ticket ticket = ticketQuery.findOrThrow(ticketId, actor);

        final List<AttachmentResponse> responses = new ArrayList<>();
        final List<String> tracksUploadedPathsForFallback = new ArrayList<>();

        try {
            for (final MultipartFile file : files) {
                validator.validateFile(file);

                final String storedPath = fileStorageService.store(ticketId, file);
                tracksUploadedPathsForFallback.add(storedPath);

                final Attachment attachment =
                        Attachment.builder()
                                .ticket(ticket)
                                .uploader(actor)
                                .filename(Objects.requireNonNull(file.getOriginalFilename()))
                                .contentType(file.getContentType())
                                .sizeBytes(file.getSize())
                                .storagePath(storedPath)
                                .build();

                final Attachment savedAttachment = attachmentRepository.save(attachment);

                attachmentMetrics.incrementUploaded();
                attachmentMetrics.recordUploadedSize(savedAttachment.getSizeBytes());

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

                responses.add(attachmentMapper.toAttachmentResponse(savedAttachment));
            }
        } catch (final RuntimeException ex) {
            // Hardened Fail-Safe: If DB save crashes mid-batch, purge written physical storage
            // files
            log.error(
                    "Transaction exception hit during attachment save processing. "
                            + "Initiating physical storage fallback purge.",
                    ex);
            for (final String fileToDelete : tracksUploadedPathsForFallback) {
                try {
                    fileStorageService.delete(fileToDelete);
                } catch (final RuntimeException deleteEx) {
                    log.error(
                            "Failed to clean up orphan file from storage path: {}",
                            fileToDelete,
                            deleteEx);
                }
            }
            throw ex; // Re-throw to accurately trigger Spring database transaction rollback
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachments(final Long ticketId, final User actor) {

        return attachmentQuery.findByTicketIdSecurely(ticketId, actor).stream()
                .map(attachmentMapper::toAttachmentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Attachment getAttachmentById(final Long attachmentId, final User actor) {
        final Attachment attachment = attachmentQuery.findOrThrow(attachmentId, actor);

        attachmentMetrics.incrementDownloaded();

        auditPublisher.publishAudit(
                AuditLog.EntityType.ATTACHMENT,
                attachment.getId(),
                actor,
                AuditLog.AuditAction.ATTACHMENT_DOWNLOADED,
                null,
                attachment.getFilename(),
                "Downloaded to ticket #" + attachment.getTicket().getId());

        return attachment;
    }

    @Override
    @Transactional
    public void deleteAttachment(final Long attachmentId, final User actor) {
        final Attachment attachment = attachmentQuery.findOrThrow(attachmentId, actor);
        final Ticket parentTicket = attachment.getTicket();

        // Security Update: Evaluate ownership boundaries across parent ticket context
        final boolean isAdmin = actor.getRole() == User.Role.ADMIN;
        final boolean isUploader = attachment.getUploader().getId().equals(actor.getId());
        final boolean isTicketRequester = parentTicket.getRequester().getId().equals(actor.getId());
        final boolean isTicketAssignee =
                parentTicket.getAssignee() != null
                        && parentTicket.getAssignee().getUser().getId().equals(actor.getId());

        if (!isAdmin && !isUploader && !isTicketRequester && !isTicketAssignee) {
            throw new AccessDeniedException(
                    "You do not have administrative or owner access privileges to "
                            + "drop this attachment");
        }

        auditPublisher.publishAudit(
                AuditLog.EntityType.ATTACHMENT,
                attachment.getId(),
                actor,
                AuditLog.AuditAction.ATTACHMENT_DELETED,
                attachment.getFilename(),
                null,
                "Deleted from ticket #" + attachment.getTicket().getId());

        fileStorageService.delete(attachment.getStoragePath());
        attachmentRepository.delete(attachment);

        attachmentMetrics.incrementDeleted();
    }
}
