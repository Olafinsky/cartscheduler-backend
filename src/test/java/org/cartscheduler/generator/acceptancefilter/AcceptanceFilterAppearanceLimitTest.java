package org.cartscheduler.generator.acceptancefilter;

import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcceptanceFilterAppearanceLimitTest {
    @Test
    void acceptsCandidateBelowLimitAndRejectsAtLimit() {
        Candidate candidate = new Candidate(1);
        SubSheet subSheet = new SubSheet(1, 3);
        subSheet.getCell(0, 0).setCandidate(candidate);
        AcceptanceFilterAppearanceLimit filter = new AcceptanceFilterAppearanceLimit(1, 2);

        assertTrue(filter.isAccepted(candidate, new Sheet(1, 1, 3), subSheet, subSheet.getCell(0, 1)));

        subSheet.getCell(0, 1).setCandidate(candidate);
        assertFalse(filter.isAccepted(candidate, new Sheet(1, 1, 3), subSheet, subSheet.getCell(0, 2)));
    }

    @Test
    void doesNotLimitOtherCandidates() {
        Candidate candidate = new Candidate(2);
        SubSheet subSheet = new SubSheet(1, 1);

        assertTrue(new AcceptanceFilterAppearanceLimit(1, 0)
            .isAccepted(candidate, new Sheet(1, 1, 1), subSheet, subSheet.getCell(0, 0)));
    }
}

