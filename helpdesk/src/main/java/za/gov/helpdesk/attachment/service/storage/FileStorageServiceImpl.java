package za.gov.helpdesk.attachment.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService{

    @Value("${app.upload.storage-path}")
    private String storagePath;

    @Override
    public String store(Long ticketId, MultipartFile file) {
        try {
            Path ticketDir   = Paths.get(storagePath, "ticket-" + ticketId);
            Files.createDirectories(ticketDir);

            String uniqueName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path   destination = ticketDir.resolve(uniqueName);
            file.transferTo(destination.toFile());

            return destination.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + file.getOriginalFilename(), e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Files.deleteIfExists(Paths.get(storagePath));
        } catch (IOException e) {
            log.error("Failed to delete file at path={} error={}", storagePath, e.getMessage());
        }
    }
}
