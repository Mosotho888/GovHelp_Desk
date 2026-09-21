package za.gov.helpdesk.reporting.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Usage metrics for a single knowledge base article, used to judge whether it is actually helping.
 */
@Setter
@Getter
@Builder
public class KnowledgeBaseEffectivenessReportRow {
    private Long articleId;
    private String title;
    private String status;
    private long viewCount;
    private long helpfulCount;
    private long notHelpfulCount;
    private long usageCount;

    /**
     * Percentage of feedback votes that were helpful, or null when the article has no votes yet.
     */
    private Double helpfulRatio;
}
