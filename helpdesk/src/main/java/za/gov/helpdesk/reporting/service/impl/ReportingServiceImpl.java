package za.gov.helpdesk.reporting.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import za.gov.helpdesk.reporting.dto.response.AgentWorkloadReportRow;
import za.gov.helpdesk.reporting.dto.response.AssetSummaryReportRow;
import za.gov.helpdesk.reporting.dto.response.CategoryBreakdownReportRow;
import za.gov.helpdesk.reporting.dto.response.KnowledgeBaseEffectivenessReportRow;
import za.gov.helpdesk.reporting.dto.response.SlaComplianceReportRow;
import za.gov.helpdesk.reporting.dto.response.TicketVolumeReportRow;
import za.gov.helpdesk.reporting.repository.ReportingRepository;
import za.gov.helpdesk.reporting.service.ReportingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportingServiceImpl implements ReportingService {

    private final ReportingRepository reportingRepository;

    @Override
    public List<TicketVolumeReportRow> getTicketVolume(final int days) {
        final LocalDate to = LocalDate.now();
        final LocalDate from = to.minusDays(days);
        return reportingRepository.findTicketVolume(from, to);
    }

    @Override
    public List<SlaComplianceReportRow> getSlaCompliance(final int days) {
        final LocalDate to = LocalDate.now();
        final LocalDate from = to.minusDays(days);
        return reportingRepository.findSlaCompliance(from, to);
    }

    @Override
    public List<AgentWorkloadReportRow> getAgentWorkload() {
        return reportingRepository.findAgentWorkload();
    }

    @Override
    public List<CategoryBreakdownReportRow> getCategoryBreakdown() {
        return reportingRepository.findCategoryBreakdown();
    }

    @Override
    public List<AssetSummaryReportRow> getAssetSummary() {
        return reportingRepository.findAssetSummary();
    }

    @Override
    public List<KnowledgeBaseEffectivenessReportRow> getKnowledgeBaseEffectiveness() {
        return reportingRepository.findKnowledgeBaseEffectiveness();
    }

    @Override
    public void refreshNow() {
        reportingRepository.refreshAll();
    }
}
