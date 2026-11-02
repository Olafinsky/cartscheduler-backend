package org.cartscheduler.generator.scorefilter;

import java.util.List;
import org.cartscheduler.generator.AcceptanceFilter;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Generator;
import org.cartscheduler.generator.OccurrenceScoreRule;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import org.cartscheduler.generator.acceptancefilter.AcceptanceFilterUniqueInRow;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreFilterPairAssignmentScoresTest {

    private static final List<OccurrenceScoreRule> DECLINING_RULES = List.of(
            new OccurrenceScoreRule(1, 1, 10),
            new OccurrenceScoreRule(2, 2, 7),
            new OccurrenceScoreRule(3, 3, 4),
            new OccurrenceScoreRule(4, 4, 1),
            new OccurrenceScoreRule(5, 100, 0)
    );

    @Test
    void assignsScorePerOccurrenceAcrossAllSubSheets() {
        SubSheet firstDay = pairDay(1L, 2L);
        SubSheet secondDay = pairDay(1L, 2L);
        SubSheet thirdDay = pairDay(1L, 3L);
        Sheet sheet = new Sheet(List.of(firstDay, secondDay, thirdDay));
        ScoreFilterPairAssignmentScores filter = new ScoreFilterPairAssignmentScores(DECLINING_RULES);

        assertThat(filter.calculateScore(sheet, firstDay)).isEqualTo(10);
        assertThat(filter.calculateScore(sheet, secondDay)).isEqualTo(7);
        assertThat(filter.calculateScore(sheet, thirdDay)).isEqualTo(10);
    }

    @Test
    void rewardsABalancedDistributionOfOtherwiseAvailablePairs() {
        ScoreFilterPairAssignmentScores filter = new ScoreFilterPairAssignmentScores(DECLINING_RULES);
        SubSheet concentrated = pairsDay(8, 1);
        SubSheet balanced = pairsDay(4, 5);

        int concentratedScore = filter.calculateScore(new Sheet(List.of(concentrated)), concentrated);
        int balancedScore = filter.calculateScore(new Sheet(List.of(balanced)), balanced);

        assertThat(concentratedScore).isEqualTo(32);
        assertThat(balancedScore).isEqualTo(44);
        assertThat(balancedScore).isGreaterThan(concentratedScore);
    }

    @Test
    void choosesALessUsedPairWhenItIsOtherwiseEquivalent() {
        SubSheet firstDay = new SubSheet(2, 1);
        firstDay.getCell(0, 0).setFixedCandidate(new Candidate(1L));
        firstDay.getCell(1, 0).setFixedCandidate(new Candidate(2L));
        SubSheet secondDay = new SubSheet(2, 1);
        secondDay.getCell(0, 0).setFixedCandidate(new Candidate(1L));

        Sheet generated = Generator.forSheet(new Sheet(List.of(firstDay, secondDay)))
                .withCandidates(List.of(new Candidate(2L), new Candidate(3L)))
                .withAcceptanceFilters(List.<AcceptanceFilter>of(new AcceptanceFilterUniqueInRow()))
                .withScoreFilters(List.of(new ScoreFilterPairAssignmentScores(DECLINING_RULES)))
                .withEquivalentRowPositions()
                .generate();

        assertThat(generated.getSubSheets().get(1).getCell(1, 0).getCandidate().getId()).isEqualTo(3L);
    }

    private SubSheet pairDay(long firstCandidateId, long secondCandidateId) {
        SubSheet day = new SubSheet(2, 1);
        day.getCell(0, 0).setCandidate(new Candidate(firstCandidateId));
        day.getCell(1, 0).setCandidate(new Candidate(secondCandidateId));
        return day;
    }

    private SubSheet pairsDay(int firstPairCount, int secondPairCount) {
        SubSheet day = new SubSheet(2, firstPairCount + secondPairCount);
        for (int row = 0; row < firstPairCount; row++) {
            day.getCell(0, row).setCandidate(new Candidate(1L));
            day.getCell(1, row).setCandidate(new Candidate(2L));
        }
        for (int row = firstPairCount; row < firstPairCount + secondPairCount; row++) {
            day.getCell(0, row).setCandidate(new Candidate(1L));
            day.getCell(1, row).setCandidate(new Candidate(3L));
        }
        return day;
    }
}
