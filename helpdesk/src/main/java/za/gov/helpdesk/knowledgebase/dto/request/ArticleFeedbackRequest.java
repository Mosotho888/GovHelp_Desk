package za.gov.helpdesk.knowledgebase.dto.request;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ArticleFeedbackRequest {

    @NotNull(message = "helpful is required")
    private Boolean helpful;
}
