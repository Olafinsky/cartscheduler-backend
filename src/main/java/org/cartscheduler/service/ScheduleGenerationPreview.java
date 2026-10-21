package org.cartscheduler.service;

/** Result of a schedule-generation dry run that did not change the database. */
public record ScheduleGenerationPreview(
        long scheduleId,
        int assignedParticipantCells,
        int score,
        boolean searchDeadlineReached
) {
}
