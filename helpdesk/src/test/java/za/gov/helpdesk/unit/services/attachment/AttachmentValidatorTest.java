package za.gov.helpdesk.unit.services.attachment;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import za.gov.helpdesk.attachment.policy.AttachmentValidator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttachmentValidator unit tests")
@SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
class AttachmentValidatorTest {

    private final AttachmentValidator validator = new AttachmentValidator();

    @Mock private MultipartFile oversizedFile;

    // ---- validateBatch ----

    @Test
    @DisplayName("validateBatch() rejects a null file list")
    void validateBatch_nullList_throws() {
        assertThatThrownBy(() -> validator.validateBatch(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No files provided");
    }

    @Test
    @DisplayName("validateBatch() rejects an empty file list")
    void validateBatch_emptyList_throws() {
        assertThatThrownBy(() -> validator.validateBatch(Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No files provided");
    }

    @Test
    @DisplayName("validateBatch() rejects a batch exceeding the maximum file count")
    void validateBatch_tooManyFiles_throws() {
        final List<MultipartFile> files =
                Collections.nCopies(AttachmentValidator.MAX_FILES + 1, pdfFile("a.pdf"));

        assertThatThrownBy(() -> validator.validateBatch(files))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "Maximum " + AttachmentValidator.MAX_FILES + " files allowed");
    }

    @Test
    @DisplayName("validateBatch() accepts a batch at exactly the maximum file count")
    void validateBatch_exactlyMaxFiles_doesNotThrow() {
        final List<MultipartFile> files =
                Collections.nCopies(AttachmentValidator.MAX_FILES, pdfFile("a.pdf"));

        assertThatCode(() -> validator.validateBatch(files)).doesNotThrowAnyException();
    }

    // ---- validateFile ----

    @Test
    @DisplayName("validateFile() rejects an empty file")
    void validateFile_empty_throws() {
        final MultipartFile empty =
                new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> validator.validateFile(empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is empty");
    }

    @Test
    @DisplayName("validateFile() rejects a file over the 20 MB limit")
    void validateFile_oversized_throws() {
        given(oversizedFile.isEmpty()).willReturn(false);
        given(oversizedFile.getSize()).willReturn(AttachmentValidator.MAX_FILE_SIZE + 1);
        given(oversizedFile.getOriginalFilename()).willReturn("huge.pdf");

        assertThatThrownBy(() -> validator.validateFile(oversizedFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds the 20 MB limit");
    }

    @Test
    @DisplayName("validateFile() accepts a file exactly at the size limit")
    void validateFile_exactlyAtSizeLimit_doesNotThrow() {
        final byte[] content = new byte[1024];
        final MultipartFile file =
                new MockMultipartFile("file", "ok.pdf", "application/pdf", content) {
                    @Override
                    public long getSize() {
                        return AttachmentValidator.MAX_FILE_SIZE;
                    }
                };

        assertThatCode(() -> validator.validateFile(file)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateFile() rejects a null content type")
    void validateFile_nullContentType_throws() {
        final MultipartFile file = new MockMultipartFile("file", "a.pdf", null, new byte[] {1});

        assertThatThrownBy(() -> validator.validateFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not allowed");
    }

    @Test
    @DisplayName("validateFile() rejects a disallowed content type")
    void validateFile_disallowedContentType_throws() {
        final MultipartFile file =
                new MockMultipartFile(
                        "file", "malware.exe", "application/x-msdownload", new byte[] {1});

        assertThatThrownBy(() -> validator.validateFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not allowed");
    }

    @Test
    @DisplayName("validateFile() accepts every explicitly whitelisted content type")
    void validateFile_allowedContentTypes_allAccepted() {
        final List<String> allowed =
                List.of(
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

        for (final String type : allowed) {
            final MultipartFile file = new MockMultipartFile("file", "doc", type, new byte[] {1});
            assertThatCode(() -> validator.validateFile(file))
                    .as("content type %s should be allowed", type)
                    .doesNotThrowAnyException();
        }
    }

    private MultipartFile pdfFile(final String filename) {
        return new MockMultipartFile("file", filename, "application/pdf", new byte[] {1});
    }
}
