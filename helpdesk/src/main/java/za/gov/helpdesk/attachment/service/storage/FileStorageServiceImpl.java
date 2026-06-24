package za.gov.helpdesk.attachment.service.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import za.gov.helpdesk.exception.StorageException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private static final Pattern SAFE_FILENAME = Pattern.compile("[^a-zA-Z0-9._-]");

    @Value("${app.upload.storage-path}")
    private String uploadRoot;

    @Override
    public String store(final Long ticketId, final MultipartFile file) {
        try {
            final Path root = Paths.get(uploadRoot).toAbsolutePath().normalize();
            final Path ticketDir = root.resolve("ticket-" + ticketId);
            Files.createDirectories(ticketDir);

            final String safeName = sanitize(file.getOriginalFilename());
            final String uniqueName = UUID.randomUUID() + "_" + safeName;
            final Path destination = ticketDir.resolve(uniqueName).normalize();

            if (!destination.startsWith(root)) {
                throw new SecurityException(
                        "Attempted path traversal in filename: " + file.getOriginalFilename());
            }

            file.transferTo(destination.toFile());
            log.info("Stored file: {}", destination);

            return destination.toString();
        } catch (final IOException | InvalidPathException e) {
            throw new StorageException("Failed to store file: " + file.getOriginalFilename(), e);
        }
    }

    @Override
    public void delete(final String storagePath) {
        try {
            final Path root = Paths.get(uploadRoot).toAbsolutePath().normalize();
            final Path target = Paths.get(storagePath).toAbsolutePath().normalize();

            if (!target.startsWith(root)) {
                log.error("Refused to delete outside upload root: {}", storagePath);
                return;
            }

            Files.deleteIfExists(target);
        } catch (final IOException e) {
            log.error(
                    "Could not delete physical storage file reference at path={}", storagePath, e);
            throw new StorageException("Could not delete file from disk storage backend", e);
        }
    }

    private String sanitize(final String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload";
        }
        // Keep only the base name (no directory components)
        final String base = Paths.get(originalFilename).getFileName().toString();
        // Strip unsafe characters
        final String safe = SAFE_FILENAME.matcher(base).replaceAll("_");
        // Collapse leading dots to prevent hidden files on Linux
        return safe.replaceAll("^\\.+", "_");
    }
}
