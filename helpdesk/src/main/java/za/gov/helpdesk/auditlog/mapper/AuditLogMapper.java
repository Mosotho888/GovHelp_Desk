package za.gov.helpdesk.auditlog.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import za.gov.helpdesk.auditlog.dto.response.AuditLogResponse;
import za.gov.helpdesk.auditlog.model.AuditLog;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "entityType", expression = "java(log.getEntityType().name())")
    @Mapping(target = "action", expression = "java(log.getAction().name())")
    AuditLogResponse toAuditLogResponse(AuditLog log);
}
