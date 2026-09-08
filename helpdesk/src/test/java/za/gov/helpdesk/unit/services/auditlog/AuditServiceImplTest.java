package za.gov.helpdesk.unit.services.auditlog;

import java.net.InetAddress;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import za.gov.helpdesk.auditlog.dto.request.AuditContext;
import za.gov.helpdesk.auditlog.dto.request.AuthAuditContext;
import za.gov.helpdesk.auditlog.dto.response.AuditLogResponse;
import za.gov.helpdesk.auditlog.mapper.AuditLogMapper;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.auditlog.repository.AuditLogRepository;
import za.gov.helpdesk.auditlog.service.impl.AuditServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditServiceImpl unit tests")
class AuditServiceImplTest {

    private static final String IP_ADDRESS = InetAddress.getLoopbackAddress().getHostAddress();

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private AuditLogMapper auditLogMapper;
    @Captor private ArgumentCaptor<AuditLog> auditLogCaptor;

    private AuditServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuditServiceImpl(auditLogRepository, auditLogMapper);
    }

    // ---- log ----

    @Test
    @DisplayName("log() persists an audit entry built from every field of the context")
    void log_fullContext_persistsAllFields() {
        final AuditContext context =
                AuditContext.builder()
                        .entityType(AuditLog.EntityType.TICKET)
                        .entityId(100L)
                        .actorId(1L)
                        .actorName("Jane Agent")
                        .actorRole("AGENT")
                        .ipAddress(IP_ADDRESS)
                        .action(AuditLog.AuditAction.STATUS_CHANGED)
                        .oldValue("OPEN")
                        .newValue("IN_PROGRESS")
                        .description("Status updated")
                        .build();

        service.log(context);

        then(auditLogRepository).should(times(1)).save(auditLogCaptor.capture());
        final AuditLog saved = auditLogCaptor.getValue();
        assertThat(saved.getEntityType()).isEqualTo(AuditLog.EntityType.TICKET);
        assertThat(saved.getEntityId()).isEqualTo(100L);
        assertThat(saved.getActorId()).isEqualTo(1L);
        assertThat(saved.getActorName()).isEqualTo("Jane Agent");
        assertThat(saved.getOldValue()).isEqualTo("OPEN");
        assertThat(saved.getNewValue()).isEqualTo("IN_PROGRESS");
        assertThat(saved.getDescription()).isEqualTo("Status updated");
    }

    @Test
    @DisplayName("log() swallows a repository failure rather than propagating it")
    void log_repositoryThrows_doesNotPropagate() {
        final AuditContext context =
                AuditContext.builder()
                        .entityType(AuditLog.EntityType.TICKET)
                        .entityId(100L)
                        .action(AuditLog.AuditAction.STATUS_CHANGED)
                        .build();
        given(auditLogRepository.save(any(AuditLog.class)))
                .willThrow(new RuntimeException("database unavailable"));

        assertThatCode(() -> service.log(context)).doesNotThrowAnyException();
    }

    // ---- logAuth ----

    @Test
    @DisplayName("logAuth() persists an AUTH-typed entry keyed by the actor's own id")
    void logAuth_fullContext_persistsAsAuthEntity() {
        final AuthAuditContext context =
                AuthAuditContext.builder()
                        .action(AuditLog.AuditAction.LOGIN_SUCCESS)
                        .actorId(1L)
                        .actorName("Jane Agent")
                        .actorRole("AGENT")
                        .ipAddress(IP_ADDRESS)
                        .description("Successful login")
                        .build();

        service.logAuth(context);

        then(auditLogRepository).should(times(1)).save(auditLogCaptor.capture());
        final AuditLog saved = auditLogCaptor.getValue();
        assertThat(saved.getEntityType()).isEqualTo(AuditLog.EntityType.AUTH);
        assertThat(saved.getEntityId()).isEqualTo(1L);
        assertThat(saved.getActorId()).isEqualTo(1L);
        assertThat(saved.getAction()).isEqualTo(AuditLog.AuditAction.LOGIN_SUCCESS);
    }

    @Test
    @DisplayName("logAuth() swallows a repository failure rather than propagating it")
    void logAuth_repositoryThrows_doesNotPropagate() {
        final AuthAuditContext context =
                AuthAuditContext.builder().action(AuditLog.AuditAction.LOGIN_FAILED).build();
        given(auditLogRepository.save(any(AuditLog.class)))
                .willThrow(new RuntimeException("database unavailable"));

        assertThatCode(() -> service.logAuth(context)).doesNotThrowAnyException();
    }

    // ---- read paths ----

    @Test
    @DisplayName("getLogsForEntity() maps every matching entry to a response")
    void fetchLogsForEntity_matchingEntity_mapsAllEntries() {
        final AuditLog entry = AuditLog.builder().id(1L).build();
        final AuditLogResponse response = AuditLogResponse.builder().build();
        given(
                        auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                                AuditLog.EntityType.TICKET, 100L))
                .willReturn(List.of(entry));
        given(auditLogMapper.toAuditLogResponse(entry)).willReturn(response);

        final List<AuditLogResponse> result =
                service.getLogsForEntity(AuditLog.EntityType.TICKET, 100L);

        assertThat(result).containsExactly(response);
    }

    @Test
    @DisplayName("getLogsByActor() maps a paginated result to responses")
    void fetchLogsByActor_anyActor_mapsPage() {
        final AuditLog entry = AuditLog.builder().id(1L).build();
        final AuditLogResponse response = AuditLogResponse.builder().build();
        final Pageable pageable = PageRequest.of(0, 10);
        given(auditLogRepository.findByActorIdOrderByCreatedAtDesc(1L, pageable))
                .willReturn(new PageImpl<>(List.of(entry)));
        given(auditLogMapper.toAuditLogResponse(entry)).willReturn(response);

        assertThat(service.getLogsByActor(1L, pageable).getContent()).containsExactly(response);
    }

    @Test
    @DisplayName("getAuthLogs() filters to AUTH-typed entries only")
    void fetchAuthLogs_anyRequest_filtersToAuthEntityType() {
        final Pageable pageable = PageRequest.of(0, 10);
        given(
                        auditLogRepository.findByEntityTypeOrderByCreatedAtDesc(
                                AuditLog.EntityType.AUTH, pageable))
                .willReturn(new PageImpl<>(List.of()));

        assertThat(service.getAuthLogs(pageable).getContent()).isEmpty();
        then(auditLogRepository)
                .should(times(1))
                .findByEntityTypeOrderByCreatedAtDesc(AuditLog.EntityType.AUTH, pageable);
    }

    @Test
    @DisplayName("getLogsByAction() delegates straight to the repository's action-based query")
    void fetchLogsByAction_anyAction_delegatesToRepository() {
        final Pageable pageable = PageRequest.of(0, 10);
        given(
                        auditLogRepository.findByActionOrderByCreatedAtDesc(
                                AuditLog.AuditAction.LOGIN_FAILED, pageable))
                .willReturn(new PageImpl<>(List.of()));

        assertThat(
                        service.getLogsByAction(AuditLog.AuditAction.LOGIN_FAILED, pageable)
                                .getContent())
                .isEmpty();
    }
}
