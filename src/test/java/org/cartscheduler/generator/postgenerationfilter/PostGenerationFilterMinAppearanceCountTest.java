package org.cartscheduler.generator.postgenerationfilter;

import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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

    @Test
    void appliesMinimumsOnlyToTheirConfiguredSubSheet() {
        SubSheet firstDay = new SubSheet(1, 2);
        SubSheet secondDay = new SubSheet(1, 2);
        firstDay.getCell(0, 0).setCandidate(new Candidate(1));
        secondDay.getCell(0, 0).setCandidate(new Candidate(1));
        secondDay.getCell(0, 1).setCandidate(new Candidate(1));
        Sheet sheet = new Sheet(List.of(firstDay, secondDay));
        PostGenerationFilterMinAppearanceCount filter = new PostGenerationFilterMinAppearanceCount(Map.of(
                0, Map.of(1L, 1),
                1, Map.of(1L, 2)
        ));

        assertTrue(filter.isAccepted(sheet, firstDay));
        assertTrue(filter.isAccepted(sheet, secondDay));
        secondDay.getCell(0, 1).setCandidate(null);
        assertFalse(filter.isAccepted(sheet, secondDay));
    }
}

