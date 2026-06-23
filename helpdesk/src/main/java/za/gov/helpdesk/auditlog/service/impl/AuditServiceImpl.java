package za.gov.helpdesk.auditlog.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.auditlog.dto.request.AuditContext;
import za.gov.helpdesk.auditlog.dto.request.AuthAuditContext;
import za.gov.helpdesk.auditlog.dto.response.AuditLogResponse;
import za.gov.helpdesk.auditlog.mapper.AuditLogMapper;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.auditlog.repository.AuditLogRepository;
import za.gov.helpdesk.auditlog.service.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(final AuditContext context) {
        try {
            final AuditLog entry =
                    AuditLog.builder()
                            .entityType(context.getEntityType())
                            .entityId(context.getEntityId())
                            .actorId(context.getActorId())
                            .actorName(context.getActorName())
                            .actorRole(context.getActorRole())
                            .ipAddress(context.getIpAddress())
                            .action(context.getAction())
                            .oldValue(context.getOldValue())
                            .newValue(context.getNewValue())
                            .description(context.getDescription())
                            .build();

            auditLogRepository.save(entry);
        } catch (final Exception e) {
            log.error(
                    "Failed to write audit log: entity={}/{} action={}",
                    context.getEntityType(),
                    context.getEntityId(),
                    context.getAction(),
                    e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuth(AuthAuditContext context) {
        try {
            final AuditLog entry =
                    AuditLog.builder()
                            .entityType(AuditLog.EntityType.AUTH)
                            .entityId(context.getActorId())
                            .actorId(context.getActorId())
                            .actorName(context.getActorName())
                            .actorRole(context.getActorRole())
                            .ipAddress(context.getIpAddress())
                            .action(context.getAction())
                            .description(context.getDescription())
                            .build();

            auditLogRepository.save(entry);
        } catch (final Exception e) {
            log.error("Failed to write auth audit log: action={}", context.getAction(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getLogsForEntity(
            final AuditLog.EntityType entityType, final Long entityId) {

        return auditLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .map(auditLogMapper::toAuditLogResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsByActor(final Long actorId, final Pageable pageable) {

        return auditLogRepository
                .findByActorIdOrderByCreatedAtDesc(actorId, pageable)
                .map(auditLogMapper::toAuditLogResponse);
    }

    @Override
    public Page<AuditLogResponse> getAuthLogs(final Pageable pageable) {

        return auditLogRepository
                .findByEntityTypeOrderByCreatedAtDesc(AuditLog.EntityType.AUTH, pageable)
                .map(auditLogMapper::toAuditLogResponse);
    }

    @Override
    public Page<AuditLogResponse> getLogsByAction(
            final AuditLog.AuditAction action, final Pageable pageable) {

        return auditLogRepository
                .findByActionOrderByCreatedAtDesc(action, pageable)
                .map(auditLogMapper::toAuditLogResponse);
    }
}
