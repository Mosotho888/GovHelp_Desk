package za.gov.helpdesk.unit.services.sla;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import za.gov.helpdesk.sla.service.BusinessHoursCalculator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName("BusinessHoursCalculator unit tests")
class BusinessHoursCalculatorTest {

    private final BusinessHoursCalculator calculator = new BusinessHoursCalculator();

    @Test
    @DisplayName("addBusinessMinutes() stays within the same day when there's enough time left")
    void addBusinessMinutes_sameDayFits_addsDirectly() {
        // Monday 2024-01-01 10:00, add 60 minutes -> 11:00 same day
        final LocalDateTime start = LocalDateTime.of(2024, 1, 1, 10, 0);

        final LocalDateTime result = calculator.addBusinessMinutes(start, 60);

        assertThat(result).isEqualTo(LocalDateTime.of(2024, 1, 1, 11, 0));
    }

    @Test
    @DisplayName(
            "addBusinessMinutes() rolls over to the next business day when minutes exceed the day")
    void addBusinessMinutes_exceedsDay_rollsToNextDay() {
        // Monday 2024-01-01 16:00, business day ends 17:00 (60 min left today).
        // Add 120 minutes -> 60 used today, 60 remaining -> next day 08:00 + 60min = 09:00
        final LocalDateTime start = LocalDateTime.of(2024, 1, 1, 16, 0);

        final LocalDateTime result = calculator.addBusinessMinutes(start, 120);

        assertThat(result).isEqualTo(LocalDateTime.of(2024, 1, 2, 9, 0));
    }

    @Test
    @DisplayName("addBusinessMinutes() skips over a weekend entirely")
    void addBusinessMinutes_spanningWeekend_skipsToMonday() {
        // Friday 2024-01-05 16:30 (30 min left today). Add 60 minutes.
        // 30 used today -> 30 remaining -> Saturday/Sunday skipped -> Monday 08:00 + 30min = 08:30
        final LocalDateTime start = LocalDateTime.of(2024, 1, 5, 16, 30);

        final LocalDateTime result = calculator.addBusinessMinutes(start, 60);

        assertThat(result).isEqualTo(LocalDateTime.of(2024, 1, 8, 8, 30));
    }

    @Test
    @DisplayName("addBusinessMinutes() snaps a weekend start forward to Monday opening")
    void addBusinessMinutes_startsOnWeekend_snapsToMonday() {
        // Saturday 2024-01-06, any time -> should snap to Monday 08:00 before adding
        final LocalDateTime start = LocalDateTime.of(2024, 1, 6, 12, 0);

        final LocalDateTime result = calculator.addBusinessMinutes(start, 30);

        assertThat(result).isEqualTo(LocalDateTime.of(2024, 1, 8, 8, 30));
    }

    @Test
    @DisplayName("addBusinessMinutes() snaps a pre-opening start forward to that morning's opening")
    void addBusinessMinutes_beforeOpening_snapsToOpeningSameDay() {
        // Monday 2024-01-01 06:00 (before 08:00 opening) -> snaps to 08:00, then +15min
        final LocalDateTime start = LocalDateTime.of(2024, 1, 1, 6, 0);

        final LocalDateTime result = calculator.addBusinessMinutes(start, 15);

        assertThat(result).isEqualTo(LocalDateTime.of(2024, 1, 1, 8, 15));
    }

    @Test
    @DisplayName("addBusinessMinutes() snaps a post-closing start forward to the next business day")
    void addBusinessMinutes_afterClosing_snapsToNextDay() {
        // Monday 2024-01-01 18:00 (after 17:00 closing) -> snaps to Tuesday 08:00, then +10min
        final LocalDateTime start = LocalDateTime.of(2024, 1, 1, 18, 0);

        final LocalDateTime result = calculator.addBusinessMinutes(start, 10);

        assertThat(result).isEqualTo(LocalDateTime.of(2024, 1, 2, 8, 10));
    }

    @Test
    @DisplayName("addBusinessMinutes() handles a multi-day span crossing more than one day")
    void addBusinessMinutes_multiDaySpan_accumulatesAcrossDays() {
        // Monday 2024-01-01 08:00: a business day is 540 minutes (08:00-17:00).
        // Adding 1110 minutes = 2 full days (1080) + 30 minutes -> Wednesday 08:30.
        final LocalDateTime start = LocalDateTime.of(2024, 1, 1, 8, 0);

        final LocalDateTime result = calculator.addBusinessMinutes(start, 1110);

        assertThat(result).isEqualTo(LocalDateTime.of(2024, 1, 3, 8, 30));
    }

    @Test
    @DisplayName("addBusinessMinutes() with zero minutes returns the snapped start time unchanged")
    void addBusinessMinutes_zeroMinutes_returnsSnappedStart() {
        final LocalDateTime start = LocalDateTime.of(2024, 1, 1, 10, 0);

        final LocalDateTime result = calculator.addBusinessMinutes(start, 0);

        assertThat(result).isEqualTo(start);
    }
}
