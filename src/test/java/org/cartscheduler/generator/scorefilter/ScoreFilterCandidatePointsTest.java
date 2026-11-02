package org.cartscheduler.generator.scorefilter;

import java.util.Map;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreFilterCandidatePointsTest {
    @Test
    void addsConfiguredPointsForEveryAppearanceOfACandidate() {
        SubSheet subSheet = new SubSheet(2, 1);
        subSheet.getCell(0, 0).setCandidate(new Candidate(1));
        subSheet.getCell(1, 0).setCandidate(new Candidate(2));

        int score = new ScoreFilterCandidatePoints(Map.of(1L, 5, 2L, -2))
                .calculateScore(new Sheet(java.util.List.of(subSheet)), subSheet);

        assertEquals(3, score);
    }
}
