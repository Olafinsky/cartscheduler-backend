package org.cartscheduler.generator.scorefilter;

import java.util.Map;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.CandidatePair;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreFilterPairPointsTest {
    @Test
    void awardsPointsForTheConfiguredPairRegardlessOfColumnOrder() {
        SubSheet subSheet = new SubSheet(2, 1);
        subSheet.getCell(0, 0).setCandidate(new Candidate(2));
        subSheet.getCell(1, 0).setCandidate(new Candidate(1));

        int score = new ScoreFilterPairPoints(Map.of(new CandidatePair(1, 2), 7))
                .calculateScore(new Sheet(java.util.List.of(subSheet)), subSheet);

        assertEquals(7, score);
    }
}
