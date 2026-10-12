package org.cartscheduler.generator.acceptancefilter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcceptanceFilterCandidateQualifiesForRowsTest {
    @Test
    void restrictsConfiguredCandidateToQualifiedRows() {
        SubSheet subSheet = new SubSheet(1, 3);
        Sheet sheet = new Sheet(1, 1, 3);
        AcceptanceFilterCandidateQualifiesForRows filter =
            new AcceptanceFilterCandidateQualifiesForRows(1, List.of(0, 2));

        assertTrue(filter.isAccepted(new Candidate(1), sheet, subSheet, subSheet.getCell(0, 2)));
        assertFalse(filter.isAccepted(new Candidate(1), sheet, subSheet, subSheet.getCell(0, 1)));
        assertTrue(filter.isAccepted(new Candidate(2), sheet, subSheet, subSheet.getCell(0, 1)));
    }

    @Test
    void usesSeparateQualifiedRowsForEachSubSheet() {
        SubSheet firstDay = new SubSheet(1, 2);
        SubSheet secondDay = new SubSheet(1, 2);
        Sheet sheet = new Sheet(List.of(firstDay, secondDay));
        AcceptanceFilterCandidateQualifiesForRows filter = new AcceptanceFilterCandidateQualifiesForRows(Map.of(
                0, Map.of(1L, Set.of(0)),
                1, Map.of(1L, Set.of(1))
        ));

        assertTrue(filter.isAccepted(new Candidate(1), sheet, firstDay, firstDay.getCell(0, 0)));
        assertFalse(filter.isAccepted(new Candidate(1), sheet, firstDay, firstDay.getCell(0, 1)));
        assertTrue(filter.isAccepted(new Candidate(1), sheet, secondDay, secondDay.getCell(0, 1)));
        assertFalse(filter.isAccepted(new Candidate(1), sheet, secondDay, secondDay.getCell(0, 0)));
    }
}

