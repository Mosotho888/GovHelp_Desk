package za.gov.helpdesk.attachment.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String store(Long ticketId, MultipartFile file);

    void delete(String storagePath);
}
