package org.cartscheduler.generator;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OccurrenceScoreRuleTest {

    @Test
    void returnsPointsForTheMatchingOrdinalOccurrence() {
        List<OccurrenceScoreRule> rules = OccurrenceScoreRule.sortedAndValidated(List.of(
                new OccurrenceScoreRule(1, 1, 10),
                new OccurrenceScoreRule(2, 3, 4)
        ));

        assertThat(OccurrenceScoreRule.pointsFor(rules, 1)).isEqualTo(10);
        assertThat(OccurrenceScoreRule.pointsFor(rules, 2)).isEqualTo(4);
        assertThat(OccurrenceScoreRule.pointsFor(rules, 4)).isZero();
    }

    @Test
    void rejectsOverlappingOccurrenceRanges() {
        assertThatThrownBy(() -> OccurrenceScoreRule.sortedAndValidated(List.of(
                new OccurrenceScoreRule(1, 2, 10),
                new OccurrenceScoreRule(2, 3, 5)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not overlap");
    }

    @Test
    void detectsWhetherAnyRuleCanAffectTheScore() {
        assertThat(OccurrenceScoreRule.hasNonZeroPoints(List.of(
                new OccurrenceScoreRule(1, 3, 0)
        ))).isFalse();
        assertThat(OccurrenceScoreRule.hasNonZeroPoints(List.of(
                new OccurrenceScoreRule(1, 1, 0),
                new OccurrenceScoreRule(2, 2, -1)
        ))).isTrue();
    }
}
