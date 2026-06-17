package za.gov.helpdesk.attachment.policy;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AttachmentValidator {

    public static final long KB = 1024L;
    public static final long ONE_MB = 1024L * KB;
    public static final long MAX_FILE_SIZE = 20 * ONE_MB; // 20 MB
    public static final int MAX_FILES = 5;

    private static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg", "image/gif", "application/pdf",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain", "text/csv", "application/zip");

    public void validateBatch(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided");
        }
        if (files.size() > MAX_FILES) {
            throw new IllegalArgumentException(
                    "Maximum " + MAX_FILES + " files allowed per request. Received: " + files.size());
        }
    }

    public void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File '" + file.getOriginalFilename() + "' is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File '" + file.getOriginalFilename()
                    + "' exceeds the 20 MB limit. Size: " + (file.getSize() / ONE_MB) + " MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("File type '" + contentType + "' is not allowed. "
                    + "Allowed types: PNG, JPG, GIF, PDF, DOC, DOCX, XLS, XLSX, TXT, CSV, ZIP");
        }
    }
}
