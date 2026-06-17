package za.gov.helpdesk.agent.mapper;

import org.mapstruct.Mapper;
import za.gov.helpdesk.agent.dto.response.AgentResponse;
import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.users.mapper.UserMapper;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface AgentMapper {

    AgentResponse toAgentResponse(Agent agent);
}
