package org.cartscheduler.generator.postgenerationfilter;

import java.util.Set;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.CandidatePair;
import org.cartscheduler.generator.PairRules;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostGenerationFilterPairRulesTest {
    @Test
    void rejectsARequiredCandidateLeftAloneAndAcceptsItsRequiredPartner() {
        SubSheet subSheet = new SubSheet(2, 1);
        Sheet sheet = new Sheet(java.util.List.of(subSheet));
        PostGenerationFilterPairRules filter = new PostGenerationFilterPairRules(new PairRules(
                Set.of(new CandidatePair(1, 2)),
                Set.of()
        ));
        subSheet.getCell(0, 0).setCandidate(new Candidate(1));

        assertFalse(filter.isAccepted(sheet, subSheet));

        subSheet.getCell(1, 0).setCandidate(new Candidate(2));
        assertTrue(filter.isAccepted(sheet, subSheet));
    }

    @Test
    void rejectsForbiddenPairsEvenWhenBothCellsAreFilled() {
        SubSheet subSheet = new SubSheet(2, 1);
        Sheet sheet = new Sheet(java.util.List.of(subSheet));
        PostGenerationFilterPairRules filter = new PostGenerationFilterPairRules(new PairRules(
                Set.of(),
                Set.of(new CandidatePair(1, 2))
        ));
        subSheet.getCell(0, 0).setCandidate(new Candidate(1));
        subSheet.getCell(1, 0).setCandidate(new Candidate(2));

        assertFalse(filter.isAccepted(sheet, subSheet));
    }
}
