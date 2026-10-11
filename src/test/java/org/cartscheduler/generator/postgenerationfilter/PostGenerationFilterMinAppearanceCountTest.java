package org.cartscheduler.generator.postgenerationfilter;

import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostGenerationFilterMinAppearanceCountTest {
    @Test
    void acceptsWhenCandidateMeetsMinimumAndRejectsBelowMinimum() {
        Candidate candidate = new Candidate(1);
        SubSheet subSheet = new SubSheet(1, 3);
        subSheet.getCell(0, 0).setCandidate(candidate);
        subSheet.getCell(0, 1).setCandidate(candidate);
        PostGenerationFilterMinAppearanceCount filter = new PostGenerationFilterMinAppearanceCount(1, 2);

        assertTrue(filter.isAccepted(new Sheet(1, 1, 3), subSheet));

        subSheet.getCell(0, 1).setCandidate(null);
        assertFalse(filter.isAccepted(new Sheet(1, 1, 3), subSheet));
    }
}

