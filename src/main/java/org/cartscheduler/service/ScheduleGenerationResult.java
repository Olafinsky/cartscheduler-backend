package org.cartscheduler.service;

/** Summary returned after a schedule has been generated and persisted. */
public record ScheduleGenerationResult(long scheduleId, int assignedParticipantCells) {
}
