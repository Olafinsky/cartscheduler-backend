package org.cartscheduler.generator;

public interface AcceptanceFilter {
    boolean isAccepted(Candidate candidate, Sheet sheet, SubSheet subSheet, Cell cell);
}

