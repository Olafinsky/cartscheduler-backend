package org.cartscheduler.generator.acceptancefilter;

import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcceptanceFilterUniqueInRowTest {
    @Test
    void rejectsCandidateAlreadyInRowButAllowsOtherRowsAndCandidates() {
        Candidate candidate = new Candidate(1);
        SubSheet subSheet = new SubSheet(2, 2);
        subSheet.getCell(0, 0).setCandidate(candidate);
        AcceptanceFilterUniqueInRow filter = new AcceptanceFilterUniqueInRow();
        Sheet sheet = new Sheet(1, 2, 2);

        assertFalse(filter.isAccepted(candidate, sheet, subSheet, subSheet.getCell(1, 0)));
        assertTrue(filter.isAccepted(candidate, sheet, subSheet, subSheet.getCell(1, 1)));
        assertTrue(filter.isAccepted(new Candidate(2), sheet, subSheet, subSheet.getCell(1, 0)));
    }
}

