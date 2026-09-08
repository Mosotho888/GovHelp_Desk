package za.gov.helpdesk.unit.services.attachment;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import za.gov.helpdesk.attachment.service.storage.FileStorageServiceImpl;
import za.gov.helpdesk.exception.StorageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileStorageServiceImpl unit tests")
class FileStorageServiceImplTest {

    private FileStorageServiceImpl storageService;

    @TempDir private Path uploadRoot;

    @BeforeEach
    void setUp() {
        storageService = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(storageService, "uploadRoot", uploadRoot.toString());
    }

    @Test
    @DisplayName("store() writes the file under a per-ticket subdirectory")
    void store_validFile_writesUnderTicketDirectory() {
        final MultipartFile file =
                new MockMultipartFile(
                        "file",
                        "report.pdf",
                        "application/pdf",
                        "content".getBytes(StandardCharsets.UTF_8));

        final String storedPath = storageService.store(42L, file);

        final Path path = Path.of(storedPath);
        assertThat(path).exists();
        assertThat(path.getParent().getFileName().toString()).isEqualTo("ticket-42");
    }

    @Test
    @DisplayName("store() preserves the readable filename while prefixing a unique id")
    void store_validFile_prefixesUniqueIdButKeepsOriginalName() {
        final MultipartFile file =
                new MockMultipartFile(
                        "file",
                        "report.pdf",
                        "application/pdf",
                        "content".getBytes(StandardCharsets.UTF_8));

        final String storedPath = storageService.store(1L, file);
        final String storedFilename = Path.of(storedPath).getFileName().toString();

        assertThat(storedFilename).endsWith("_report.pdf");
        assertThat(storedFilename).matches("^[0-9a-fA-F-]{36}_report\\.pdf$");
    }

    @Test
    @DisplayName("store() strips directory separators from a filename to prevent path traversal")
    void store_pathTraversalFilename_sanitizesToBaseName() {
        final MultipartFile file =
                new MockMultipartFile(
                        "file",
                        "../../etc/passwd",
                        "text/plain",
                        "malicious".getBytes(StandardCharsets.UTF_8));

        final String storedPath = storageService.store(1L, file);

        // Only the base name "passwd" should survive; the traversal segments are stripped, and the
        // resulting file must still land inside this ticket's own directory.
        final Path path = Path.of(storedPath);
        assertThat(path.getFileName().toString()).endsWith("_passwd");
        assertThat(path.getParent().getFileName().toString()).isEqualTo("ticket-1");
    }

    @Test
    @DisplayName("store() replaces unsafe characters in the filename")
    void store_unsafeCharactersInFilename_replacesWithUnderscore() {
        final MultipartFile file =
                new MockMultipartFile(
                        "file",
                        "weird name!@#.txt",
                        "text/plain",
                        "content".getBytes(StandardCharsets.UTF_8));

        final String storedPath = storageService.store(1L, file);
        final String storedFilename = Path.of(storedPath).getFileName().toString();

        assertThat(storedFilename).doesNotContain("!", "@", "#", " ");
    }

    @Test
    @DisplayName("store() falls back to a default name when the original filename is blank")
    void store_blankFilename_fallsBackToDefaultName() {
        final MultipartFile file =
                new MockMultipartFile(
                        "file", "", "text/plain", "content".getBytes(StandardCharsets.UTF_8));

        final String storedPath = storageService.store(1L, file);

        assertThat(Path.of(storedPath).getFileName().toString()).endsWith("_upload");
    }

    @Test
    @DisplayName("store() collapses a leading dot to avoid creating a hidden file")
    void store_leadingDotFilename_collapsesToUnderscore() {
        final MultipartFile file =
                new MockMultipartFile(
                        "file",
                        "...bashrc",
                        "text/plain",
                        "content".getBytes(StandardCharsets.UTF_8));

        final String storedPath = storageService.store(1L, file);
        final String storedFilename = Path.of(storedPath).getFileName().toString();

        assertThat(storedFilename).doesNotStartWith(".");
    }

    @Test
    @DisplayName("store() wraps a transfer failure in a StorageException")
    void store_transferFails_wrapsInStorageException() {
        final MultipartFile failingFile =
                new MockMultipartFile(
                        "file",
                        "report.pdf",
                        "application/pdf",
                        "content".getBytes(StandardCharsets.UTF_8)) {
                    @Override
                    public void transferTo(final @NonNull File dest) throws IOException {
                        throw new IOException("disk full");
                    }
                };

        assertThatThrownBy(() -> storageService.store(1L, failingFile))
                .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("delete() removes a file that exists inside the upload root")
    void delete_existingFileInsideRoot_removesFile() throws IOException {
        final Path ticketDir = uploadRoot.resolve("ticket-1");
        Files.createDirectories(ticketDir);
        final Path file = ticketDir.resolve("some-file.pdf");
        Files.writeString(file, "content");

        storageService.delete(file.toString());

        assertThat(file).doesNotExist();
    }

    @Test
    @DisplayName("delete() is a silent no-op for a path that does not exist")
    void delete_missingFile_doesNotThrow() {
        final Path missing = uploadRoot.resolve("ticket-1").resolve("missing.pdf");

        assertThatCode(() -> storageService.delete(missing.toString())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("delete() refuses to delete a path outside the configured upload root")
    void delete_pathOutsideRoot_refusesAndLeavesFileIntact(@TempDir final Path outsideDir)
            throws IOException {
        final Path outsideFile = outsideDir.resolve("sensitive.txt");
        Files.writeString(outsideFile, "do not delete me");

        storageService.delete(outsideFile.toString());

        assertThat(outsideFile).exists();
    }
}
