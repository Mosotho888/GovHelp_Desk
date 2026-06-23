package za.gov.helpdesk.sla.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

/**
 * Service component responsible for calculating operational times based on official business hours.
 * Enforces a strict schedule spanning Monday through Friday, 08:00 to 17:00, automatically skipping
 * weekends and shifting out-of-bounds operational request markers into the next valid business day
 * window.
 */
@Component
public class BusinessHoursCalculator {

    private static final LocalTime BUSINESS_START = LocalTime.of(8, 0);
    private static final LocalTime BUSINESS_END = LocalTime.of(17, 0);
    private static final int DAYS_TO_ADD = 1;

    /**
     * Increments a baseline timestamp by a given duration of active business minutes, safely
     * jumping over nights and weekends. If the initial start time lands outside operational hours,
     * it automatically snaps forward to the closest business opening gate before processing the
     * remaining time allocation.
     *
     * @param start the initial {@link LocalDateTime} timestamp reference checkpoint
     * @param businessMinutes the total number of operational business minutes to add
     * @return the resulting {@link LocalDateTime} representing the absolute deadline timestamp
     */
    public LocalDateTime addBusinessMinutes(final LocalDateTime start, final long businessMinutes) {
        LocalDateTime current = snapToBusinessHours(start);
        long remaining = businessMinutes;

        while (remaining > 0) {
            if (isWeekend(current)) {
                current = nextBusinessDayStart(current);
                continue;
            }

            final LocalDateTime endOfDay = current.toLocalDate().atTime(BUSINESS_END);
            final long minutesLeftToday = Duration.between(current, endOfDay).toMinutes();

            if (remaining <= minutesLeftToday) {
                current = current.plusMinutes(remaining);
                remaining = 0;
            } else {
                remaining -= minutesLeftToday;
                current = nextBusinessDayStart(current);
            }
        }

        return current;
    }

    /**
     * Evaluates a timestamp and normalizes it to sit within valid operating hour boundaries.
     * Weekends and evening timestamps automatically skip forward to the next business day's opening
     * gate, while pre-market early morning timestamps shift directly to that morning's start time.
     *
     * @param dt the raw incoming {@link LocalDateTime} instance to inspect
     * @return a normalized {@link LocalDateTime} shifted into operational boundaries
     */
    private LocalDateTime snapToBusinessHours(final LocalDateTime dt) {
        if (isWeekend(dt)) {
            return nextBusinessDayStart(dt);
        }
        if (dt.toLocalTime().isBefore(BUSINESS_START)) {
            return dt.toLocalDate().atTime(BUSINESS_START);
        }
        if (!dt.toLocalTime().isBefore(BUSINESS_END)) {
            return nextBusinessDayStart(dt);
        }
        return dt;
    }

    /**
     * Calculates the absolute opening boundary timestamp (08:00) of the immediate next valid
     * business day. Iterates incrementally through calendar dates until hitting a standard weekday.
     *
     * @param dt the reference {@link LocalDateTime} boundary date to roll forward from
     * @return a {@link LocalDateTime} anchored exactly at the next business day's start time
     */
    private LocalDateTime nextBusinessDayStart(final LocalDateTime dt) {
        LocalDateTime next = dt.toLocalDate().plusDays(DAYS_TO_ADD).atTime(BUSINESS_START);
        while (isWeekend(next)) {
            next = next.toLocalDate().plusDays(DAYS_TO_ADD).atTime(BUSINESS_START);
        }
        return next;
    }

    /**
     * Checks if a specific date timestamp falls on a weekend.
     *
     * @param dt the target {@link LocalDateTime} parameter to inspect
     * @return true if the timestamp represents a Saturday or Sunday, false otherwise
     */
    private boolean isWeekend(final LocalDateTime dt) {
        final DayOfWeek day = dt.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
