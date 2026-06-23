package za.gov.helpdesk.auditlog.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import za.gov.helpdesk.auditlog.dto.messaging.AuditLogMessage;
import za.gov.helpdesk.auditlog.dto.request.AuditContext;
import za.gov.helpdesk.auditlog.dto.response.AuditLogResponse;
import za.gov.helpdesk.auditlog.model.AuditLog;

/**
 * Data mapping component responsible for translating structural audit trail records, asynchronous
 * messaging event broker objects, and database models into serialized API responses.
 */
@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    /**
     * Translates a persistent database audit log model entity into a flattened data transfer
     * snapshot matching the outbound REST API presentation response format.
     *
     * @param log the source persistent database domain entity tracking system metrics
     * @return a mapped and structured outbound {@link AuditLogResponse} payload
     */
    @Mapping(target = "entityType", expression = "java(log.getEntityType().name())")
    @Mapping(target = "action", expression = "java(log.getAction().name())")
    AuditLogResponse toAuditLogResponse(AuditLog log);

    /**
     * Converts an inbound messaging queue payload event into a structural audit logging parameter
     * context.
     *
     * @param message the source queue message payload tracking raw state changes
     * @return the verified target {@link AuditContext} parameter wrapper instance
     */
    AuditContext toAuditContext(AuditLogMessage message);
}
