package org.cartscheduler.service.generation;

/** Summary returned after a schedule has been generated and persisted. */
public record ScheduleGenerationResult(long scheduleId, int assignedParticipantCells) {
}
