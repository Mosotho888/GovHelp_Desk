package za.gov.helpdesk.sla.service;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class BusinessHoursCalculator {

    private static final LocalTime BUSINESS_START = LocalTime.of(8, 0);
    private static final LocalTime BUSINESS_END   = LocalTime.of(17, 0);
    private static final long       BUSINESS_MINUTES_PER_DAY = 9 * 60;

    public LocalDateTime addBusinessMinutes(LocalDateTime start, long businessMinutes) {
        LocalDateTime current = snapToBusinessHours(start);
        long remaining = businessMinutes;

        while (remaining > 0) {
            if (isWeekend(current)) {
                current = nextBusinessDayStart(current);
                continue;
            }

            LocalDateTime endOfDay = current.toLocalDate().atTime(BUSINESS_END);
            long minutesLeftToday = Duration.between(current, endOfDay).toMinutes();

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

    private LocalDateTime snapToBusinessHours(LocalDateTime dt) {
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

    private LocalDateTime nextBusinessDayStart(LocalDateTime dt) {
        LocalDateTime next = dt.toLocalDate().plusDays(1).atTime(BUSINESS_START);
        while (isWeekend(next)) {
            next = next.toLocalDate().plusDays(1).atTime(BUSINESS_START);
        }
        return next;
    }

    private boolean isWeekend(LocalDateTime dt) {
        DayOfWeek day = dt.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
