package za.gov.helpdesk.unit.reporting;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.gov.helpdesk.reporting.dto.response.AgentWorkloadReportRow;
import za.gov.helpdesk.reporting.dto.response.AssetSummaryReportRow;
import za.gov.helpdesk.reporting.dto.response.CategoryBreakdownReportRow;
import za.gov.helpdesk.reporting.dto.response.KnowledgeBaseEffectivenessReportRow;
import za.gov.helpdesk.reporting.dto.response.SlaComplianceReportRow;
import za.gov.helpdesk.reporting.dto.response.TicketVolumeReportRow;
import za.gov.helpdesk.reporting.repository.ReportingRepository;
import za.gov.helpdesk.reporting.service.impl.ReportingServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for ReportingServiceImpl. The repository itself (ReportingRepository) issues raw SQL
 * against the reporting materialized views and is best covered by an integration test against a
 * real Postgres instance rather than a mocked JdbcTemplate, since a mock cannot verify that the SQL
 * is actually correct. These tests cover the service layer's own responsibility instead: the days
 * to date range conversion, and delegating each report to the repository unchanged.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReportingServiceImpl unit tests")
class ReportingServiceImplTest {

    @Mock private ReportingRepository reportingRepository;

    private ReportingServiceImpl reportingService;

    @BeforeEach
    void setUp() {
        reportingService = new ReportingServiceImpl(reportingRepository);
    }

    @Nested
    @DisplayName("getTicketVolume()")
    class GetTicketVolume {

        @Test
        @DisplayName("converts the days parameter into a [today - days, today] date range")
        void getTicketVolume_daysParam_computesCorrectDateRange() {
            given(reportingRepository.findTicketVolume(any(), any())).willReturn(List.of());

            final LocalDate expectedTo = LocalDate.now();
            reportingService.getTicketVolume(30);

            final ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
            final ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
            verify(reportingRepository).findTicketVolume(fromCaptor.capture(), toCaptor.capture());

            assertThat(toCaptor.getValue()).isEqualTo(expectedTo);
            assertThat(fromCaptor.getValue()).isEqualTo(expectedTo.minusDays(30));
        }

        @Test
        @DisplayName("returns exactly what the repository returns")
        void getTicketVolume_delegatesToRepository_returnsResult() {
            final List<TicketVolumeReportRow> rows =
                    List.of(TicketVolumeReportRow.builder().status("OPEN").ticketCount(5).build());
            given(reportingRepository.findTicketVolume(any(), any())).willReturn(rows);

            final List<TicketVolumeReportRow> result = reportingService.getTicketVolume(7);

            assertThat(result).isEqualTo(rows);
        }

        @Test
        @DisplayName("a zero day window still resolves to a single-day [today, today] range")
        void getTicketVolume_zeroDays_rangeIsTodayOnly() {
            given(reportingRepository.findTicketVolume(any(), any())).willReturn(List.of());

            final LocalDate expectedTo = LocalDate.now();
            reportingService.getTicketVolume(0);

            verify(reportingRepository).findTicketVolume(expectedTo, expectedTo);
        }
    }

    @Nested
    @DisplayName("getSlaCompliance()")
    class GetSlaCompliance {

        @Test
        @DisplayName("converts the days parameter into a [today - days, today] date range")
        void getSlaCompliance_daysParam_computesCorrectDateRange() {
            given(reportingRepository.findSlaCompliance(any(), any())).willReturn(List.of());

            final LocalDate expectedTo = LocalDate.now();
            reportingService.getSlaCompliance(90);

            final ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
            final ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
            verify(reportingRepository).findSlaCompliance(fromCaptor.capture(), toCaptor.capture());

            assertThat(toCaptor.getValue()).isEqualTo(expectedTo);
            assertThat(fromCaptor.getValue()).isEqualTo(expectedTo.minusDays(90));
        }

        @Test
        @DisplayName("returns exactly what the repository returns")
        void getSlaCompliance_delegatesToRepository_returnsResult() {
            final List<SlaComplianceReportRow> rows =
                    List.of(SlaComplianceReportRow.builder().totalCount(10).metCount(8).build());
            given(reportingRepository.findSlaCompliance(any(), any())).willReturn(rows);

            final List<SlaComplianceReportRow> result = reportingService.getSlaCompliance(30);

            assertThat(result).isEqualTo(rows);
        }
    }

    @Nested
    @DisplayName("reports with no date range")
    class ReportsWithoutDateRange {

        @Test
        @DisplayName("getAgentWorkload() delegates to the repository unchanged")
        void getAgentWorkload_delegatesToRepository() {
            final List<AgentWorkloadReportRow> rows =
                    List.of(
                            AgentWorkloadReportRow.builder()
                                    .agentId(1L)
                                    .agentName("Agent One")
                                    .build());
            given(reportingRepository.findAgentWorkload()).willReturn(rows);

            final List<AgentWorkloadReportRow> result = reportingService.getAgentWorkload();

            assertThat(result).isEqualTo(rows);
        }

        @Test
        @DisplayName("getCategoryBreakdown() delegates to the repository unchanged")
        void getCategoryBreakdown_delegatesToRepository() {
            final List<CategoryBreakdownReportRow> rows =
                    List.of(
                            CategoryBreakdownReportRow.builder()
                                    .categoryId(1L)
                                    .categoryName("Hardware")
                                    .build());
            given(reportingRepository.findCategoryBreakdown()).willReturn(rows);

            final List<CategoryBreakdownReportRow> result = reportingService.getCategoryBreakdown();

            assertThat(result).isEqualTo(rows);
        }

        @Test
        @DisplayName("getAssetSummary() delegates to the repository unchanged")
        void getAssetSummary_delegatesToRepository() {
            final List<AssetSummaryReportRow> rows =
                    List.of(
                            AssetSummaryReportRow.builder()
                                    .type("LAPTOP")
                                    .status("IN_USE")
                                    .build());
            given(reportingRepository.findAssetSummary()).willReturn(rows);

            final List<AssetSummaryReportRow> result = reportingService.getAssetSummary();

            assertThat(result).isEqualTo(rows);
        }

        @Test
        @DisplayName("getKnowledgeBaseEffectiveness() delegates to the repository unchanged")
        void getKnowledgeBaseEffectiveness_delegatesToRepository() {
            final List<KnowledgeBaseEffectivenessReportRow> rows =
                    List.of(
                            KnowledgeBaseEffectivenessReportRow.builder()
                                    .articleId(1L)
                                    .title("VPN Access")
                                    .build());
            given(reportingRepository.findKnowledgeBaseEffectiveness()).willReturn(rows);

            final List<KnowledgeBaseEffectivenessReportRow> result =
                    reportingService.getKnowledgeBaseEffectiveness();

            assertThat(result).isEqualTo(rows);
        }
    }

    @Nested
    @DisplayName("refreshNow()")
    class RefreshNow {

        @Test
        @DisplayName("delegates to the repository's refreshAll()")
        void refreshNow_delegatesToRepository() {
            reportingService.refreshNow();

            verify(reportingRepository).refreshAll();
        }
    }
}
