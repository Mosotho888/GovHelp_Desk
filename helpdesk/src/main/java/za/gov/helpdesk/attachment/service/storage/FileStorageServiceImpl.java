package za.gov.helpdesk.attachment.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService{

    private static final Pattern SAFE_FILENAME = Pattern.compile("[^a-zA-Z0-9._-]");

    @Value("${app.upload.storage-path}")
    private String uploadRoot;

    @Override
    public String store(Long ticketId, MultipartFile file) {
        try {
            Path root = Paths.get(uploadRoot).toAbsolutePath().normalize();
            Path ticketDir   = root.resolve("ticket-" + ticketId);
            Files.createDirectories(ticketDir);

            String safeName    = sanitize(file.getOriginalFilename());
            String uniqueName  = UUID.randomUUID() + "_" + safeName;
            Path   destination = ticketDir.resolve(uniqueName).normalize();

            if (!destination.startsWith(root)) {
                throw new SecurityException(
                        "Attempted path traversal in filename: "
                                + file.getOriginalFilename());
            }

            file.transferTo(destination.toFile());
            log.info("Stored file: {}", destination);

            return destination.toString();
        } catch (IOException | InvalidPathException e) {
            throw new RuntimeException("Failed to store file: " + file.getOriginalFilename(), e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Path root   = Paths.get(uploadRoot).toAbsolutePath().normalize();
            Path target = Paths.get(storagePath).toAbsolutePath().normalize();

            if (!target.startsWith(root)) {
                log.error("Refused to delete outside upload root: {}", storagePath);
                return;
            }

            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.error("Failed to delete file at path={} error={}", storagePath, e.getMessage());
        }
    }

    private String sanitize(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload";
        }
        // Keep only the base name (no directory components)
        String base = Paths.get(originalFilename).getFileName().toString();
        // Strip unsafe characters
        String safe = SAFE_FILENAME.matcher(base).replaceAll("_");
        // Collapse leading dots to prevent hidden files on Linux
        return safe.replaceAll("^\\.+", "_");
    }
}
