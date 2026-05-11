package za.gov.helpdesk.attachment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import za.gov.helpdesk.attachment.dto.AttachmentResponse;
import za.gov.helpdesk.attachment.model.Attachment;
import za.gov.helpdesk.attachment.service.AttachmentService;
import za.gov.helpdesk.ticket.exception.TicketNotFoundException;
import za.gov.helpdesk.users.dto.UserResponse;
import za.gov.helpdesk.users.model.User;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {
    @Override
    @Transactional
    public List<AttachmentResponse> uploadAttachments(Long ticketId, List<MultipartFile> files) {
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachments(Long ticketId) {
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentResponse getAttachmentById(Long attachmentId) {
        return null;
    }

    @Override
    @Transactional
    public void deleteAttachment(Long attachmentId) {

    }

    private Attachment findOrThrow(Long id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException());
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new TicketNotFoundException());
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
