package za.gov.helpdesk.attachment.controller;

import java.nio.file.Paths;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import za.gov.helpdesk.attachment.dto.response.AttachmentResponse;
import za.gov.helpdesk.attachment.model.Attachment;
import za.gov.helpdesk.attachment.service.AttachmentService;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.users.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for handling ticket file attachments. Provides endpoints for uploading, listing,
 * downloading, and deleting files.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "File attachments on tickets")
@SecurityRequirement(name = "bearerAuth")
public class AttachmentController {
    private final AttachmentService attachmentService;

    /**
     * Uploads multiple file attachments and associates them with a specific ticket. Enforces limits
     * of a maximum of 5 files, up to 20 MB each.
     *
     * @param ticketId the unique identifier of the target ticket
     * @param files the list of multipart files to upload
     * @param principal the authenticated user context performing the operation
     * @return a {@link ResponseEntity} containing a list of {@link AttachmentResponse} metadata
     *     wrappers
     */
    @PostMapping(
            value = "/v1/tickets/{ticketId}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload files to a ticket (max 5 files, 20 MB each)")
    public ResponseEntity<List<AttachmentResponse>> uploadAttachments(
            @PathVariable final Long ticketId,
            @RequestParam("file") final List<MultipartFile> files,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.uploadAttachments(ticketId, files, principal.getUser()));
    }

    /**
     * Retrieves metadata for all file attachments linked to a specific ticket.
     *
     * @param ticketId the unique identifier of the target ticket
     * @param principal the authenticated user context performing the operation
     * @return a {@link ResponseEntity} containing the list of associated attachments
     */
    @GetMapping("/v1/tickets/{ticketId}/attachments")
    @Operation(summary = "List all attachments on a ticket")
    public ResponseEntity<List<AttachmentResponse>> getAttachments(
            @PathVariable final Long ticketId,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.ok(attachmentService.getAttachments(ticketId, principal.getUser()));
    }

    /**
     * Downloads the raw binary file stream of an attachment by its unique identifier.
     *
     * @param attachmentId the unique identifier of the target attachment
     * @param principal the authenticated user context performing the operation
     * @return a {@link ResponseEntity} wrapping the binary {@link Resource} file stream
     * @throws ResourceNotFoundException if the attachment record exists but the file is missing
     *     from local storage
     */
    @GetMapping("/v1/attachments/{attachmentId}")
    @Operation(summary = "Download an attachment by ID")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable final Long attachmentId,
            @AuthenticationPrincipal final CustomUserDetails principal) {

        final Attachment attachment =
                attachmentService.getAttachmentById(attachmentId, principal.getUser());

        final Resource resource = new FileSystemResource(Paths.get(attachment.getStoragePath()));
        if (!resource.exists()) {
            throw new ResourceNotFoundException(
                    "File not found on storage for attachment " + attachmentId);
        }

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .body(resource);
    }

    /**
     * Deletes an attachment record and removes its physical file binary from disk storage.
     *
     * @param attachmentId the unique identifier of the attachment to delete
     * @param principal the authenticated user context performing the operation
     */
    @DeleteMapping("/v1/attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an attachment")
    public void deleteAttachment(
            @PathVariable final Long attachmentId,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        attachmentService.deleteAttachment(attachmentId, principal.getUser());
    }
}
