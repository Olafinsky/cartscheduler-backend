package org.cartscheduler.generator.scorefilter;

import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreFilterRowFullOrEmptyTest {
    @Test
    void scoresFullRowsAdjacentToIncompleteRowsAndEmptyRows() {
        SubSheet subSheet = new SubSheet(2, 4);
        subSheet.getCell(0, 0).setCandidate(new Candidate(1));
        subSheet.getCell(1, 0).setCandidate(new Candidate(2));
        subSheet.getCell(0, 1).setCandidate(new Candidate(3));
        subSheet.getCell(0, 3).setCandidate(new Candidate(4));
        subSheet.getCell(1, 3).setCandidate(new Candidate(5));

        assertEquals(5, new ScoreFilterRowFullOrEmpty(1, 2, 3)
                .calculateScore(new Sheet(1, 2, 4), subSheet));
    }

    @Test
    void givesThreePointsForFullRowsNotAdjacentToIncompleteRows() {
        SubSheet subSheet = new SubSheet(2, 2);
        subSheet.getCell(0, 0).setCandidate(new Candidate(1));
        subSheet.getCell(1, 0).setCandidate(new Candidate(2));
        subSheet.getCell(0, 1).setCandidate(new Candidate(3));
        subSheet.getCell(1, 1).setCandidate(new Candidate(4));

        assertEquals(6, new ScoreFilterRowFullOrEmpty(1, 2, 3)
                .calculateScore(new Sheet(1, 2, 2), subSheet));
    }

    @Test
    void usesPointsSuppliedByTheCaller() {
        SubSheet subSheet = new SubSheet(2, 2);
        subSheet.getCell(0, 0).setCandidate(new Candidate(1));
        subSheet.getCell(1, 0).setCandidate(new Candidate(2));

        assertEquals(18, new ScoreFilterRowFullOrEmpty(7, 11, 13)
                .calculateScore(new Sheet(1, 2, 2), subSheet));
    }
}

