package za.gov.helpdesk.attachment.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import za.gov.helpdesk.attachment.dto.response.AttachmentResponse;
import za.gov.helpdesk.attachment.mapper.AttachmentMapper;
import za.gov.helpdesk.attachment.metrics.AttachmentMetrics;
import za.gov.helpdesk.attachment.model.Attachment;
import za.gov.helpdesk.attachment.policy.AttachmentValidator;
import za.gov.helpdesk.attachment.repository.AttachmentRepository;
import za.gov.helpdesk.attachment.service.AttachmentService;
import za.gov.helpdesk.attachment.service.storage.FileStorageService;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final AttachmentMapper attachmentMapper;
    private final TicketRepository ticketRepository;
    private final AuditEventPublisher auditPublisher;
    private final FileStorageService fileStorageService;
    private final AttachmentValidator validator;
    private final AttachmentMetrics attachmentMetrics;

    @Value("${app.upload.storage-path}")
    private String storagePath;

    @Override
    @Transactional
    public List<AttachmentResponse> uploadAttachments(Long ticketId, List<MultipartFile> files, User actor) {

        validator.validateBatch(files);

        Ticket ticket = ticketRepository
                .findByIdAndPrincipal(ticketId, actor.getEmail(), actor.getRole().name())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        List<AttachmentResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            validator.validateFile(file);

            String storedPath = fileStorageService.store(ticketId, file);

            Attachment attachment = Attachment.builder()
                    .ticket(ticket)
                    .uploader(actor)
                    .filename(Objects.requireNonNull(file.getOriginalFilename()))
                    .contentType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .storagePath(storedPath)
                    .build();

            Attachment savedAttachment = attachmentRepository.save(attachment);

            attachmentMetrics.incrementUploaded();
            attachmentMetrics.recordUploadedSize(savedAttachment.getSizeBytes());

            auditPublisher.publishAudit(
                    AuditLog.EntityType.ATTACHMENT,
                    savedAttachment.getId(),
                    actor,
                    AuditLog.AuditAction.ATTACHMENT_UPLOADED,
                    null,
                    savedAttachment.getFilename(),
                    "Uploaded to ticket #" + ticketId + " (" + (savedAttachment.getSizeBytes() / 1024) + " KB)"
            );

            responses.add(attachmentMapper.toAttachmentResponse(savedAttachment));
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachments(Long ticketId, User actor) {
        ticketRepository
                .findByIdAndPrincipal(ticketId, actor.getEmail(), actor.getRole().name())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        return attachmentRepository.findByTicketId(ticketId)
                .stream().map(attachmentMapper::toAttachmentResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Attachment getAttachmentById(Long attachmentId, User actor) {
        Attachment attachment = findOrThrow(attachmentId, actor);

        attachmentMetrics.incrementDownloaded();

        auditPublisher.publishAudit(
                AuditLog.EntityType.ATTACHMENT,
                attachment.getId(),
                actor,
                AuditLog.AuditAction.ATTACHMENT_DOWNLOADED,
                null,
                attachment.getFilename(),
                "Downloaded to ticket #" + attachment.getTicket().getId()
        );

        return attachment;
    }

    @Override
    @Transactional
    public void deleteAttachment(Long attachmentId, User actor) {
        Attachment attachment = findOrThrow(attachmentId, actor);

        boolean isAdmin  = actor.getRole() == User.Role.ADMIN;
        boolean isOwner  = attachment.getUploader().getId().equals(actor.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException(
                    "You can only delete your own attachments");
        }

        auditPublisher.publishAudit(
                AuditLog.EntityType.ATTACHMENT,
                attachment.getId(),
                actor,
                AuditLog.AuditAction.ATTACHMENT_DELETED,
                attachment.getFilename(),
                null,
                "Deleted from ticket #" + attachment.getTicket().getId()
        );

        fileStorageService.delete(attachment.getStoragePath());
        attachmentRepository.delete(attachment);

        attachmentMetrics.incrementDeleted();
    }

    private Attachment findOrThrow(Long attachmentId, User actor) {
        return attachmentRepository
                .findByIdForActor(attachmentId, actor.getEmail(), actor.getRole().name())
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", attachmentId));
    }
}
