package za.gov.helpdesk.unit.services.attachment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import za.gov.helpdesk.attachment.repository.AttachmentRepository;
import za.gov.helpdesk.attachment.service.AttachmentService;
import za.gov.helpdesk.attachment.service.impl.AttachmentServiceImpl;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttachmentService unit tests")
public class AttachmentServiceImplTest {
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private AttachmentServiceImpl attachmentServiceImpl;

    private User uploader;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(attachmentServiceImpl, "storagePath", "/tmp/helpdesk-test");

        uploader = User.builder().id(1L).name("Jane Agent").email("jane@gov.za")
                .role(User.Role.AGENT).active(true).build();
        ticket = Ticket.builder().id(10L).subject("Test").description("desc")
                .status(Ticket.Status.OPEN).requester(uploader)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("uploadAttachments() rejects more than 5 files")
    void upload_moreThanFiveFiles_throwsIllegalArgument() {
        List<MockMultipartFile> files = List.of(
                mockFile("a.pdf"), mockFile("b.pdf"), mockFile("c.pdf"),
                mockFile("d.pdf"), mockFile("e.pdf"), mockFile("f.pdf")
        );

        assertThatThrownBy(() -> attachmentServiceImpl.uploadAttachments(10L, List.copyOf(files)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Maximum 5 files");
    }

    @Test
    @DisplayName("uploadAttachments() rejects unsupported file types")
    void upload_unsupportedType_throwsIllegalArgument() {

        mockAuthenticatedUser();
        given(authentication.getName()).willReturn("jane@gov.za");
        given(userRepository.findByEmail("jane@gov.za")).willReturn(Optional.of(uploader));
        given(ticketRepository.findById(10L)).willReturn(Optional.of(ticket));

        MockMultipartFile exeFile = new MockMultipartFile(
                "file", "malware.exe", "application/x-msdownload", "fake content".getBytes()
        );

        assertThatThrownBy(() -> attachmentServiceImpl.uploadAttachments(10L, List.of(exeFile)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    @DisplayName("uploadAttachments() rejects files exceeding 20 MB")
    void upload_fileTooLarge_throwsIllegalArgument() {
        mockAuthenticatedUser();
        given(authentication.getName()).willReturn("jane@gov.za");
        given(userRepository.findByEmail("jane@gov.za")).willReturn(Optional.of(uploader));
        given(ticketRepository.findById(10L)).willReturn(Optional.of(ticket));

        byte[] bigContent = new byte[21 * 1024 * 1024]; // 21 MB
        MockMultipartFile bigFile = new MockMultipartFile(
                "file", "big.pdf", "application/pdf", bigContent
        );

        assertThatThrownBy(() -> attachmentServiceImpl.uploadAttachments(10L, List.of(bigFile)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20 MB");
    }

    @Test
    @DisplayName("uploadAttachments() throws ResourceNotFoundException for unknown ticket")
    void upload_unknownTicket_throwsNotFound() {
//        mockAuthenticatedUser();
        given(ticketRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> attachmentServiceImpl.uploadAttachments(999L, List.of(mockFile("a.pdf"))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteAttachment() throws when attachment not found")
    void delete_unknownAttachment_throwsNotFound() {
        given(attachmentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> attachmentServiceImpl.deleteAttachment(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private MockMultipartFile mockFile(String filename) {
        return new MockMultipartFile("file", filename, "application/pdf", "pdf content".getBytes());
    }

    private void mockAuthenticatedUser() {
        SecurityContextHolder.setContext(securityContext);
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getName()).willReturn("jane@gov.za");
    }
}
