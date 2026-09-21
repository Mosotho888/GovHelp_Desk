package za.gov.helpdesk.knowledgebase.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class ArticleFeedbackResponse {
    private Long articleId;
    private int helpfulCount;
    private int notHelpfulCount;

    /** The requesting user's own vote, or null if they haven't voted on this article. */
    private Boolean yourVote;
}
