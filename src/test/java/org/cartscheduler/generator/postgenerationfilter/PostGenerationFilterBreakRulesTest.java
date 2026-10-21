package org.cartscheduler.generator.postgenerationfilter;

import java.util.List;
import java.util.Map;
import org.cartscheduler.generator.BreakRequirement;
import org.cartscheduler.generator.BreakRule;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.IntRange;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostGenerationFilterBreakRulesTest {
    @Test
    void rejectsAGapForANoBreakRule() {
        SubSheet subSheet = new SubSheet(1, 4);
        Sheet sheet = new Sheet(List.of(subSheet));
        PostGenerationFilterBreakRules filter = new PostGenerationFilterBreakRules(Map.of(
                0, Map.of(1L, new BreakRule(BreakRequirement.NO_BREAK, null))
        ));
        Candidate candidate = new Candidate(1);

        subSheet.getCell(0, 0).setCandidate(candidate);
        subSheet.getCell(0, 1).setCandidate(candidate);
        assertTrue(filter.isAccepted(sheet, subSheet));

        subSheet.getCell(0, 3).setCandidate(candidate);
        assertFalse(filter.isAccepted(sheet, subSheet));
    }

    @Test
    void requiresABreakOfTheConfiguredLength() {
        SubSheet subSheet = new SubSheet(1, 4);
        Sheet sheet = new Sheet(List.of(subSheet));
        PostGenerationFilterBreakRules filter = new PostGenerationFilterBreakRules(Map.of(
                0, Map.of(1L, new BreakRule(BreakRequirement.REQUIRE_BREAK, new IntRange(1, 1)))
        ));
        Candidate candidate = new Candidate(1);

        subSheet.getCell(0, 0).setCandidate(candidate);
        subSheet.getCell(0, 1).setCandidate(candidate);
        assertFalse(filter.isAccepted(sheet, subSheet));

        subSheet.getCell(0, 1).setCandidate(null);
        subSheet.getCell(0, 2).setCandidate(candidate);
        assertTrue(filter.isAccepted(sheet, subSheet));

        subSheet.getCell(0, 2).setCandidate(null);
        subSheet.getCell(0, 3).setCandidate(candidate);
        assertFalse(filter.isAccepted(sheet, subSheet));
    }

    @Test
    void allowsAnIndifferentRuleToHaveNoBreakButValidatesAnExistingOne() {
        SubSheet subSheet = new SubSheet(1, 4);
        Sheet sheet = new Sheet(List.of(subSheet));
        PostGenerationFilterBreakRules filter = new PostGenerationFilterBreakRules(Map.of(
                0, Map.of(1L, new BreakRule(BreakRequirement.INDIFFERENT, new IntRange(1, 1)))
        ));
        Candidate candidate = new Candidate(1);

        subSheet.getCell(0, 0).setCandidate(candidate);
        subSheet.getCell(0, 1).setCandidate(candidate);
        assertTrue(filter.isAccepted(sheet, subSheet));

        subSheet.getCell(0, 1).setCandidate(null);
        subSheet.getCell(0, 2).setCandidate(candidate);
        assertTrue(filter.isAccepted(sheet, subSheet));

        subSheet.getCell(0, 2).setCandidate(null);
        subSheet.getCell(0, 3).setCandidate(candidate);
        assertFalse(filter.isAccepted(sheet, subSheet));
    }

    @Test
    void appliesRulesOnlyToTheirOwnSubSheet() {
        SubSheet firstDay = new SubSheet(1, 3);
        SubSheet secondDay = new SubSheet(1, 3);
        Sheet sheet = new Sheet(List.of(firstDay, secondDay));
        Candidate candidate = new Candidate(1);
        firstDay.getCell(0, 0).setCandidate(candidate);
        firstDay.getCell(0, 2).setCandidate(candidate);
        secondDay.getCell(0, 0).setCandidate(candidate);
        secondDay.getCell(0, 2).setCandidate(candidate);
        PostGenerationFilterBreakRules filter = new PostGenerationFilterBreakRules(Map.of(
                0, Map.of(1L, new BreakRule(BreakRequirement.NO_BREAK, null)),
                1, Map.of(1L, new BreakRule(BreakRequirement.REQUIRE_BREAK, new IntRange(1, 1)))
        ));

        assertFalse(filter.isAccepted(sheet, firstDay));
        assertTrue(filter.isAccepted(sheet, secondDay));
    }
}
