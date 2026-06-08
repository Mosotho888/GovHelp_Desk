package za.gov.helpdesk.unit.services.attachment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import za.gov.helpdesk.attachment.mapper.AttachmentMapper;
import za.gov.helpdesk.attachment.metrics.AttachmentMetrics;
import za.gov.helpdesk.attachment.model.Attachment;
import za.gov.helpdesk.attachment.policy.AttachmentValidator;
import za.gov.helpdesk.attachment.repository.AttachmentRepository;
import za.gov.helpdesk.attachment.service.impl.AttachmentServiceImpl;
import za.gov.helpdesk.attachment.service.storage.FileStorageService;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.users.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttachmentService unit tests")
public class AttachmentServiceImplTest {

    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private AttachmentMapper attachmentMapper;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private AuditEventPublisher auditPublisher;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private AttachmentValidator validator;
    @Mock
    private AttachmentMetrics attachmentMetrics;

    @InjectMocks
    private AttachmentServiceImpl attachmentService;

    private User uploader;
    private User adminUser;
    private Ticket ticket;
    private Attachment attachment;

    @BeforeEach
    void setUp() {

        uploader = User.builder().id(1L).name("Jane Agent").email("jane@gov.za")
                .role(User.Role.AGENT).active(true).build();
        adminUser = User.builder().id(3L).name("Admin User").email("admin@gov.za")
                .role(User.Role.ADMIN).active(true).build();
        ticket = Ticket.builder().id(10L).subject("Test").description("desc")
                .status(Ticket.Status.OPEN).requester(uploader)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        attachment = Attachment.builder()
                .id(50L).ticket(ticket).uploader(uploader)
                .filename("report.pdf").contentType("application/pdf")
                .sizeBytes(1024L).storagePath("/tmp/helpdesk-test/10/report.pdf")
                .build();
    }

    @Test
    @DisplayName("uploadAttachments() saves attachment and publishes ATTACHMENT_UPLOADED audit")
    void upload_validFile_savesAndPublishesAudit() {
        MockMultipartFile file = pdfFile("report.pdf");

        given(ticketRepository.findByIdAndPrincipal(10L, uploader.getEmail(), uploader.getRole().name()))
                .willReturn(Optional.of(ticket));
        given(fileStorageService.store(10L, file)).willReturn("/tmp/helpdesk-test/10/report.pdf");
        given(attachmentRepository.save(any(Attachment.class))).willReturn(attachment);

        attachmentService.uploadAttachments(10L, List.of(file), uploader);

        then(attachmentMetrics).should(times(1)).incrementUploaded();
        then(attachmentMetrics).should(times(1)).recordUploadedSize(attachment.getSizeBytes());
        then(attachmentRepository).should(times(1)).save(any(Attachment.class));
        then(auditPublisher).should(times(1)).publishAudit(
                eq(AuditLog.EntityType.ATTACHMENT), eq(attachment.getId()),
                eq(uploader), eq(AuditLog.AuditAction.ATTACHMENT_UPLOADED),
                isNull(), eq("report.pdf"), any()
        );
    }

    @Test
    @DisplayName("uploadAttachments() delegates batch validation to AttachmentValidator")
    void upload_delegatesBatchValidationToValidator() {
        List<MultipartFile> files = List.of(pdfFile("a.pdf"));
        willThrow(new IllegalArgumentException("Maximum 5 files allowed per request. Received: 6"))
                .given(validator).validateBatch(any());

        // The validator is called first — before any ticket lookup
        assertThatThrownBy(() -> attachmentService.uploadAttachments(10L, files, uploader))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Maximum 5 files");

        then(ticketRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("uploadAttachments() delegates per-file validation to AttachmentValidator")
    void upload_delegatesFileValidationToValidator() {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file", "malware.exe", "application/x-msdownload", "fake".getBytes());

        given(ticketRepository.findByIdAndPrincipal(10L, uploader.getEmail(), uploader.getRole().name()))
                .willReturn(Optional.of(ticket));
        willThrow(new IllegalArgumentException("File type 'application/x-msdownload' is not allowed."))
                .given(validator).validateFile(exeFile);

        assertThatThrownBy(() -> attachmentService.uploadAttachments(10L, List.of(exeFile), uploader))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");

        then(attachmentRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("uploadAttachments() throws ResourceNotFoundException for unknown ticket")
    void upload_unknownTicket_throwsNotFound() {
        given(ticketRepository.findByIdAndPrincipal(999L, uploader.getEmail(), uploader.getRole().name()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> attachmentService.uploadAttachments(999L, List.of(pdfFile("a.pdf")), uploader))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }


    @Test
    @DisplayName("getAttachmentById() returns attachment and publishes ATTACHMENT_DOWNLOADED audit")
    void getAttachmentById_valid_returnsAndPublishesAudit() {
        given(attachmentRepository.findByIdForActor(50L, uploader.getEmail(), uploader.getRole().name()))
                .willReturn(Optional.of(attachment));

        Attachment result = attachmentService.getAttachmentById(50L, uploader);

        assertThat(result.getId()).isEqualTo(50L);
        assertThat(result.getFilename()).isEqualTo("report.pdf");

        then(attachmentMetrics).should(times(1)).incrementDownloaded();
        then(auditPublisher).should(times(1)).publishAudit(
                eq(AuditLog.EntityType.ATTACHMENT), eq(50L),
                eq(uploader), eq(AuditLog.AuditAction.ATTACHMENT_DOWNLOADED),
                isNull(), eq("report.pdf"), any()
        );
    }

    @Test
    @DisplayName("getAttachmentById() throws ResourceNotFoundException for unknown attachment")
    void getAttachmentById_unknownId_throwsNotFound() {
        given(attachmentRepository.findByIdForActor(999L, uploader.getEmail(), uploader.getRole().name()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> attachmentService.getAttachmentById(999L, uploader))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }


    @Test
    @DisplayName("deleteAttachment() allows owner to delete their own attachment")
    void deleteAttachment_byOwner_deletesSuccessfully() {
        given(attachmentRepository.findByIdForActor(50L, uploader.getEmail(), uploader.getRole().name()))
                .willReturn(Optional.of(attachment));

        attachmentService.deleteAttachment(50L, uploader);

        then(attachmentMetrics).should(times(1)).incrementDeleted();
        then(fileStorageService).should(times(1)).delete(attachment.getStoragePath());
        then(attachmentRepository).should(times(1)).delete(attachment);
        then(auditPublisher).should(times(1)).publishAudit(
                eq(AuditLog.EntityType.ATTACHMENT), eq(50L),
                eq(uploader), eq(AuditLog.AuditAction.ATTACHMENT_DELETED),
                eq("report.pdf"), isNull(), any()
        );
    }

    @Test
    @DisplayName("deleteAttachment() allows admin to delete any attachment")
    void deleteAttachment_byAdmin_deletesSuccessfully() {
        Attachment otherUsersAttachment = Attachment.builder()
                .id(51L).ticket(ticket).uploader(uploader)
                .filename("other.pdf").contentType("application/pdf")
                .sizeBytes(512L).storagePath("/tmp/helpdesk-test/10/other.pdf")
                .build();

        given(attachmentRepository.findByIdForActor(51L, adminUser.getEmail(), adminUser.getRole().name()))
                .willReturn(Optional.of(otherUsersAttachment));

        attachmentService.deleteAttachment(51L, adminUser);

        then(attachmentMetrics).should(times(1)).incrementDeleted();
        then(fileStorageService).should(times(1)).delete(otherUsersAttachment.getStoragePath());
        then(attachmentRepository).should(times(1)).delete(otherUsersAttachment);
    }

    @Test
    @DisplayName("deleteAttachment() throws AccessDeniedException when non-owner non-admin tries to delete")
    void deleteAttachment_nonOwnerNonAdmin_throwsAccessDenied() {
        User otherUser = User.builder().id(99L).name("Other").email("other@gov.za")
                .role(User.Role.AGENT).active(true).build();

        // attachment.uploader is 'uploader' (id=1), but actor is 'otherUser' (id=99)
        given(attachmentRepository.findByIdForActor(50L, otherUser.getEmail(), otherUser.getRole().name()))
                .willReturn(Optional.of(attachment));

        assertThatThrownBy(() -> attachmentService.deleteAttachment(50L, otherUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("own attachments");

        then(attachmentRepository).should(never()).delete(any());
        then(fileStorageService).should(never()).delete(any());
    }

    @Test
    @DisplayName("deleteAttachment() throws ResourceNotFoundException for unknown attachment")
    void deleteAttachment_unknownId_throwsNotFound() {
        given(attachmentRepository.findByIdForActor(999L, uploader.getEmail(), uploader.getRole().name()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> attachmentService.deleteAttachment(999L, uploader))
                .isInstanceOf(ResourceNotFoundException.class);
    }


    @Test
    @DisplayName("getAttachments() throws ResourceNotFoundException for unknown ticket")
    void getAttachments_unknownTicket_throwsNotFound() {
        given(ticketRepository.findByIdAndPrincipal(999L, uploader.getEmail(), uploader.getRole().name()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> attachmentService.getAttachments(999L, uploader))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private MockMultipartFile pdfFile(String filename) {
        return new MockMultipartFile("file", filename, "application/pdf", "pdf content".getBytes());
    }
}
