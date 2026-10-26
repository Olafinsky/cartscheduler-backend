package org.cartscheduler.generator.scorefilter;

import java.util.List;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreFilterPairDiversityTest {
    @Test
    void awardsPointsOnlyForTheSubSheetContainingTheFirstOccurrenceOfAPair() {
        SubSheet firstDay = new SubSheet(2, 1);
        SubSheet secondDay = new SubSheet(2, 2);
        firstDay.getCell(0, 0).setCandidate(new Candidate(1));
        firstDay.getCell(1, 0).setCandidate(new Candidate(2));
        secondDay.getCell(0, 0).setCandidate(new Candidate(2));
        secondDay.getCell(1, 0).setCandidate(new Candidate(1));
        secondDay.getCell(0, 1).setCandidate(new Candidate(1));
        secondDay.getCell(1, 1).setCandidate(new Candidate(3));
        Sheet sheet = new Sheet(List.of(firstDay, secondDay));

        ScoreFilterPairDiversity filter = new ScoreFilterPairDiversity(4);

        assertEquals(4, filter.calculateScore(sheet, firstDay));
        assertEquals(4, filter.calculateScore(sheet, secondDay));
        assertEquals(
                8,
                sheet.getSubSheets().stream().mapToInt(day -> filter.calculateScore(sheet, day)).sum()
        );
    }
}
