package za.gov.helpdesk.auditlog.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import za.gov.helpdesk.auditlog.dto.response.AuditLogResponse;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.auditlog.repository.AuditLogRepository;
import za.gov.helpdesk.auditlog.service.AuditService;
import za.gov.helpdesk.users.model.User;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditLog.EntityType entityType, Long entityId, Long actorId, String actorName, String actorRole, String ipAddress, AuditLog.AuditAction action, String description) {
        log(entityType, entityId, actorId, actorName, actorRole, ipAddress, action, null,null, description);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditLog.EntityType entityType, Long entityId, Long actorId, String actorName, String actorRole, String ipAddress, AuditLog.AuditAction action, String oldValue, String newValue, String description) {
        try {
            AuditLog entry = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .actorId(actorId)
                    .actorName(actorName)
                    .actorRole(actorRole)
                    .ipAddress(ipAddress)
                    .action(action)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .description(description)
                    .build();

            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write audit log: entity={}/{} action={} error={}", entityType, entityId, action, e.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuth(AuditLog.AuditAction action, Long actorId, String actorName, String actorRole, String ipAddress, String description) {
        try {
            AuditLog entry = AuditLog.builder()
                    .entityType(AuditLog.EntityType.AUTH)
                    .entityId(actorId)
                    .actorName(actorName)
                    .actorRole(actorRole)
                    .ipAddress(ipAddress)
                    .action(action)
                    .description(description)
                    .build();

            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write auth audit log: action={} error={}", action, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getLogsForEntity(AuditLog.EntityType entityType, Long entityId) {

        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsByActor(Long actorId, Pageable pageable) {

        return auditLogRepository.findByActorIdOrderByCreatedAtDesc(actorId, pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<AuditLogResponse> getAuthLogs(Pageable pageable) {

        return auditLogRepository.findByEntityTypeOrderByCreatedAtDesc(AuditLog.EntityType.AUTH, pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<AuditLogResponse> getLogsByAction(AuditLog.AuditAction action, Pageable pageable) {

        return auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable)
                .map(this::toResponse);
    }

    public AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .entityType(log.getEntityType().name())
                .entityId(log.getEntityId())
                .actorId(log.getActorId())
                .actorName(log.getActorName())
                .actorRole(log.getActorRole())
                .ipAddress(log.getIpAddress())
                .action(log.getAction().name())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .description(log.getDescription())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
