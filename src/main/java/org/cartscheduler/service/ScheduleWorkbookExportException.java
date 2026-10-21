package org.cartscheduler.service;

/** Signals that an XLSX export of a persisted schedule could not be created. */
public class ScheduleWorkbookExportException extends RuntimeException {

    public ScheduleWorkbookExportException(String message) {
        super(message);
    }

    public ScheduleWorkbookExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
