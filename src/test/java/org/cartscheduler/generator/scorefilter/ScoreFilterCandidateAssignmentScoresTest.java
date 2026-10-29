package org.cartscheduler.generator.scorefilter;

import java.util.List;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Generator;
import org.cartscheduler.generator.OccurrenceScoreRule;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreFilterCandidateAssignmentScoresTest {

    @Test
    void scoresOrdinalAssignmentsAcrossAllSubSheets() {
        SubSheet firstDay = new SubSheet(1, 1);
        firstDay.getCell(0, 0).setCandidate(new Candidate(1L));
        SubSheet secondDay = new SubSheet(2, 1);
        secondDay.getCell(0, 0).setCandidate(new Candidate(1L));
        secondDay.getCell(1, 0).setCandidate(new Candidate(2L));
        Sheet sheet = new Sheet(List.of(firstDay, secondDay));
        ScoreFilterCandidateAssignmentScores filter = new ScoreFilterCandidateAssignmentScores(List.of(
                new OccurrenceScoreRule(1, 1, 10),
                new OccurrenceScoreRule(2, 2, 7)
        ));

        assertThat(filter.calculateScore(sheet, firstDay)).isEqualTo(10);
        assertThat(filter.calculateScore(sheet, secondDay)).isEqualTo(17);
    }

    @Test
    void prefersGivingTheFirstAssignmentToAnotherCandidate() {
        Sheet generated = Generator.forSheet(new Sheet(List.of(new SubSheet(1, 2))))
                .withCandidates(List.of(new Candidate(1L), new Candidate(2L)))
                .withScoreFilters(List.of(new ScoreFilterCandidateAssignmentScores(List.of(
                        new OccurrenceScoreRule(1, 1, 10),
                        new OccurrenceScoreRule(2, 100, 0)
                ))))
                .generate();

        assertThat(generated.getSubSheets().getFirst().getCells())
                .extracting(cell -> cell.getCandidate().getId())
                .containsExactlyInAnyOrder(1L, 2L);
    }
}
