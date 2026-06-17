package za.gov.helpdesk.sla.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

@Component
public class BusinessHoursCalculator {

    private static final LocalTime BUSINESS_START = LocalTime.of(8, 0);
    private static final LocalTime BUSINESS_END = LocalTime.of(17, 0);
    private static final int DAYS_TO_ADD = 1;

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
        LocalDateTime next = dt.toLocalDate().plusDays(DAYS_TO_ADD).atTime(BUSINESS_START);
        while (isWeekend(next)) {
            next = next.toLocalDate().plusDays(DAYS_TO_ADD).atTime(BUSINESS_START);
        }
        return next;
    }

    private boolean isWeekend(LocalDateTime dt) {
        DayOfWeek day = dt.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
