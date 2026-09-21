package za.gov.helpdesk.reporting.service;

import java.util.List;

import za.gov.helpdesk.reporting.dto.response.AgentWorkloadReportRow;
import za.gov.helpdesk.reporting.dto.response.AssetSummaryReportRow;
import za.gov.helpdesk.reporting.dto.response.CategoryBreakdownReportRow;
import za.gov.helpdesk.reporting.dto.response.KnowledgeBaseEffectivenessReportRow;
import za.gov.helpdesk.reporting.dto.response.SlaComplianceReportRow;
import za.gov.helpdesk.reporting.dto.response.TicketVolumeReportRow;

public interface ReportingService {

    List<TicketVolumeReportRow> getTicketVolume(int days);

    List<SlaComplianceReportRow> getSlaCompliance(int days);

    List<AgentWorkloadReportRow> getAgentWorkload();

    List<CategoryBreakdownReportRow> getCategoryBreakdown();

    List<AssetSummaryReportRow> getAssetSummary();

    List<KnowledgeBaseEffectivenessReportRow> getKnowledgeBaseEffectiveness();

    /** Refreshes every reporting view immediately, outside the normal scheduled cycle. */
    void refreshNow();
}
