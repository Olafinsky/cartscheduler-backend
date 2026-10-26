package org.cartscheduler.service.generation;

import java.time.LocalDate;

/** Summary of one calendar schedule generation attempt. */
public record ScheduleCalendarGenerationResult(
        boolean created,
        String scheduleName,
        LocalDate startDate,
        LocalDate endDate,
        int createdScheduleDays,
        int createdScheduleEntries
) {
}
