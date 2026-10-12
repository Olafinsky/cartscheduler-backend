package org.cartscheduler.generator.acceptancefilter;

import java.util.Set;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.CandidatePair;
import org.cartscheduler.generator.PairRules;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcceptanceFilterPairRulesTest {
    @Test
    void rejectsForbiddenPairsAndPartnersOutsideTheRequiredPair() {
        SubSheet subSheet = new SubSheet(2, 1);
        Sheet sheet = new Sheet(java.util.List.of(subSheet));
        AcceptanceFilterPairRules filter = new AcceptanceFilterPairRules(new PairRules(
                Set.of(new CandidatePair(1, 2)),
                Set.of(new CandidatePair(3, 4))
        ));

        subSheet.getCell(0, 0).setCandidate(new Candidate(1));
        assertTrue(filter.isAccepted(new Candidate(2), sheet, subSheet, subSheet.getCell(1, 0)));
        assertFalse(filter.isAccepted(new Candidate(3), sheet, subSheet, subSheet.getCell(1, 0)));

        subSheet.getCell(0, 0).setCandidate(new Candidate(3));
        assertFalse(filter.isAccepted(new Candidate(4), sheet, subSheet, subSheet.getCell(1, 0)));
    }
}
