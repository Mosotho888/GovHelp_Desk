package za.gov.helpdesk.unit.reporting;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.gov.helpdesk.reporting.scheduler.ReportingRefreshScheduler;
import za.gov.helpdesk.reporting.service.ReportingService;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportingRefreshScheduler unit tests")
class ReportingRefreshSchedulerTest {

    @Mock private ReportingService reportingService;

    @InjectMocks private ReportingRefreshScheduler scheduler;

    @Test
    @DisplayName("run() triggers exactly one refresh cycle")
    void run_triggersOneRefreshCycle() {
        scheduler.run();

        verify(reportingService, times(1)).refreshNow();
        verifyNoMoreInteractions(reportingService);
    }

    @Test
    @DisplayName("each invocation of run() triggers its own refresh cycle")
    void run_calledTwice_triggersTwoRefreshCycles() {
        scheduler.run();
        scheduler.run();

        verify(reportingService, times(2)).refreshNow();
    }
}
