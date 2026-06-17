package za.gov.helpdesk.unit.services.sla;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.gov.helpdesk.sla.service.BusinessHoursCalculator;

@DisplayName("BusinessHoursCalculator unit tests")
class BusinessHoursCalculatorTest {

    private final BusinessHoursCalculator calculator = new BusinessHoursCalculator();

    @Test
    @DisplayName("addBusinessMinutes() adds minutes inside same business day")
    void addBusinessMinutes_sameBusinessDay() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 27, 9, 15);

        LocalDateTime result = calculator.addBusinessMinutes(start, 90);

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 5, 27, 10, 45));
    }

    @Test
    @DisplayName("addBusinessMinutes() snaps before-hours start to 08:00")
    void addBusinessMinutes_beforeHours_snapsToBusinessStart() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 27, 6, 30);

        LocalDateTime result = calculator.addBusinessMinutes(start, 30);

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 5, 27, 8, 30));
    }

    @Test
    @DisplayName("addBusinessMinutes() snaps after-hours start to next business day")
    void addBusinessMinutes_afterHours_snapsToNextBusinessDay() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 27, 17, 0);

        LocalDateTime result = calculator.addBusinessMinutes(start, 45);

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 5, 28, 8, 45));
    }

    @Test
    @DisplayName("addBusinessMinutes() carries remaining minutes to next business day")
    void addBusinessMinutes_crossesBusinessDayBoundary() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 27, 16, 30);

        LocalDateTime result = calculator.addBusinessMinutes(start, 90);

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 5, 28, 9, 0));
    }

    @Test
    @DisplayName("addBusinessMinutes() skips weekend when starting on Saturday")
    void addBusinessMinutes_weekendStart_skipsToMonday() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 30, 10, 0);

        LocalDateTime result = calculator.addBusinessMinutes(start, 60);

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 6, 1, 9, 0));
    }

    @Test
    @DisplayName("addBusinessMinutes() skips weekend when crossing Friday evening")
    void addBusinessMinutes_fridayCrossing_skipsWeekend() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 29, 16, 30);

        LocalDateTime result = calculator.addBusinessMinutes(start, 90);

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 6, 1, 9, 0));
    }
}
