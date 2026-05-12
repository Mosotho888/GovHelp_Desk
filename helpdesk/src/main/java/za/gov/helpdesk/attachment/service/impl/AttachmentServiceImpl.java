package za.gov.helpdesk.attachment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import za.gov.helpdesk.attachment.dto.AttachmentResponse;
import za.gov.helpdesk.attachment.model.Attachment;
import za.gov.helpdesk.attachment.repository.AttachmentRepository;
import za.gov.helpdesk.attachment.service.AttachmentService;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.ticket.exception.TicketNotFoundException;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.TicketRepository;
import za.gov.helpdesk.users.dto.UserResponse;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.storage-path}")
    private String storagePath;

    private static final long   MAX_FILE_SIZE   = 20 * 1024 * 1024L; // 20 MB
    private static final int    MAX_FILES       = 5;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain", "text/csv",
            "application/zip"
    );

    @Override
    @Transactional
    public List<AttachmentResponse> uploadAttachments(Long ticketId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided");
        }
        if (files.size() > MAX_FILES) {
            throw new IllegalArgumentException(
                    "Maximum " + MAX_FILES + " files allowed per request. Received: " + files.size());
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        User uploader = getCurrentUser();

        List<AttachmentResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            validateFile(file);

            String storedPath = storeFile(ticketId, file);

            Attachment attachment = Attachment.builder()
                    .ticket(ticket)
                    .uploader(uploader)
                    .filename(Objects.requireNonNull(file.getOriginalFilename()))
                    .contentType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .storagePath(storedPath)
                    .build();

            responses.add(toResponse(attachmentRepository.save(attachment)));
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachments(Long ticketId) {
        ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        return attachmentRepository.findByTicketId(ticketId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentResponse getAttachmentById(Long attachmentId) {
        return toResponse(findOrThrow(attachmentId));
    }

    @Override
    @Transactional
    public void deleteAttachment(Long attachmentId) {
        Attachment attachment = findOrThrow(attachmentId);
        User current = getCurrentUser();

        boolean isAdmin  = current.getRole() == User.Role.ADMIN;
        boolean isOwner  = attachment.getUploader().getId().equals(current.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException(
                    "You can only delete your own attachments");
        }

        // Remove file from storage
        try {
            Files.deleteIfExists(Paths.get(attachment.getStoragePath()));
        } catch (IOException e) {
            // Log but don't block — DB record must still be removed
        }

        attachmentRepository.delete(attachment);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File '" + file.getOriginalFilename() + "' is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File '" + file.getOriginalFilename() + "' exceeds the 20 MB limit. Size: "
                            + (file.getSize() / (1024 * 1024)) + " MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "File type '" + contentType + "' is not allowed. "
                            + "Allowed types: PNG, JPG, GIF, PDF, DOC, DOCX, XLS, XLSX, TXT, CSV, ZIP");
        }
    }

    private String storeFile(Long ticketId, MultipartFile file) {
        try {
            Path ticketDir = Paths.get(storagePath, "ticket-" + ticketId);
            Files.createDirectories(ticketDir);

            String uniqueName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path destination  = ticketDir.resolve(uniqueName);
            file.transferTo(destination.toFile());

            return destination.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + file.getOriginalFilename(), e);
        }
    }

    private Attachment findOrThrow(Long id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", id));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private AttachmentResponse toResponse(Attachment a) {
        return AttachmentResponse.builder()
                .id(a.getId())
                .ticketId(a.getTicket().getId())
                .uploader(UserResponse.builder()
                        .id(a.getUploader().getId())
                        .name(a.getUploader().getName())
                        .email(a.getUploader().getEmail())
                        .role(a.getUploader().getRole())
                        .build())
                .filename(a.getFilename())
                .contentType(a.getContentType())
                .sizeBytes(a.getSizeBytes())
                .downloadUrl("/api/v1/attachments/" + a.getId())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
