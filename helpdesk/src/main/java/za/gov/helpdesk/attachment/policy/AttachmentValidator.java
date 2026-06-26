package za.gov.helpdesk.attachment.policy;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Policy engine component responsible for enforcing business validation rules on ticket file
 * attachments. Validates payload constraints including batch size, individual file size capacity,
 * and content MIME types.
 */
@Component
public class AttachmentValidator {

    public static final long KB = 1024L;
    public static final long ONE_MB = 1024L * KB;
    public static final long MAX_FILE_SIZE = 20 * ONE_MB; // 20 MB
    public static final int MAX_FILES = 5;

    private static final Set<String> ALLOWED_TYPES =
            Set.of(
                    "image/png",
                    "image/jpeg",
                    "image/gif",
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "text/plain",
                    "text/csv",
                    "application/zip");

    /**
     * Validates structural constraints on a collection batch of uploaded files. Checks that the
     * list is populated and does not exceed the total file allocation limit.
     *
     * @param files the list of {@link MultipartFile} objects requested for upload
     * @throws IllegalArgumentException if the file list is empty or exceeds {@value #MAX_FILES}
     */
    public void validateBatch(final List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided");
        }
        if (files.size() > MAX_FILES) {
            throw new IllegalArgumentException(
                    "Maximum "
                            + MAX_FILES
                            + " files allowed per request. Received: "
                            + files.size());
        }
    }

    /**
     * Validates content dimensions and safety properties of an individual file. Assures that the
     * binary stream is not blank, fits under the 20 MB size boundary, and maps strictly to an
     * authorized media type whitelist.
     *
     * @param file the individual {@link MultipartFile} instance to evaluate
     * @throws IllegalArgumentException if the file is empty, over-sized, or uses an untrusted
     *     format extension
     */
    public void validateFile(final MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File '" + file.getOriginalFilename() + "' is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File '"
                            + file.getOriginalFilename()
                            + "' exceeds the 20 MB limit. Size: "
                            + (file.getSize() / ONE_MB)
                            + " MB");
        }
        final String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "File type '"
                            + contentType
                            + "' is not allowed. Allowed types: PNG, JPG, GIF, PDF, DOC, DOCX, XLS,"
                            + " XLSX, TXT, CSV, ZIP");
        }
    }
}
