package za.gov.helpdesk.attachment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import za.gov.helpdesk.attachment.dto.response.AttachmentResponse;
import za.gov.helpdesk.attachment.model.Attachment;
import za.gov.helpdesk.users.mapper.UserMapper;

@Mapper(componentModel = "spring", uses = UserMapper.class)
@FunctionalInterface
public interface AttachmentMapper {

    @Mapping(source = "ticket.id", target = "ticketId")
    @Mapping(source = "uploader", target = "uploader")
    @Mapping(target = "downloadUrl", expression = "java(\"/v1/attachments/\" + attachment.getId())")
    AttachmentResponse toAttachmentResponse(Attachment attachment);
}
