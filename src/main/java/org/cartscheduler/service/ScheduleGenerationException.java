package org.cartscheduler.service;

/** Signals an invalid configuration or an unsatisfiable schedule-generation request. */
public class ScheduleGenerationException extends RuntimeException {
    public ScheduleGenerationException(String message) {
        super(message);
    }

    public ScheduleGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
