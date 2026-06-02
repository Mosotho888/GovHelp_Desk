package za.gov.helpdesk.attachment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import za.gov.helpdesk.attachment.dto.response.AttachmentResponse;
import za.gov.helpdesk.attachment.model.Attachment;
import za.gov.helpdesk.attachment.repository.AttachmentRepository;
import za.gov.helpdesk.attachment.service.AttachmentService;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.users.security.CustomUserDetails;

import java.nio.file.Paths;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "File attachments on tickets")
@SecurityRequirement(name = "bearerAuth")
public class AttachmentController {
    private final AttachmentService attachmentService;

    @PostMapping(
            value  = "/v1/tickets/{ticketId}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Upload files to a ticket (max 5 files, 20 MB each)")
    public ResponseEntity<List<AttachmentResponse>> uploadAttachments(
            @PathVariable Long ticketId,
            @RequestParam("file") List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.uploadAttachments(ticketId, files, principal.getUser()));
    }

    @GetMapping("/v1/tickets/{ticketId}/attachments")
    @Operation(summary = "List all attachments on a ticket")
    public ResponseEntity<List<AttachmentResponse>> getAttachments(@PathVariable Long ticketId) {
        return ResponseEntity.ok(attachmentService.getAttachments(ticketId));
    }

    @GetMapping("/v1/attachments/{attachmentId}")
    @Operation(summary = "Download an attachment by ID")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Attachment attachment =  attachmentService.getAttachmentById(attachmentId, principal.getUser());

        Resource resource = new FileSystemResource(Paths.get(attachment.getStoragePath()));
        if (!resource.exists()) {
            throw new ResourceNotFoundException("File not found on storage for attachment " + attachmentId);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .body(resource);
    }

    @DeleteMapping("/v1/attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an attachment")
    public void deleteAttachment(
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        attachmentService.deleteAttachment(attachmentId, principal.getUser());
    }
}
