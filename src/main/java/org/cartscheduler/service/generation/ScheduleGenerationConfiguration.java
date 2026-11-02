package org.cartscheduler.service.generation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.cartscheduler.generator.CandidatePair;
import org.cartscheduler.generator.IntRange;
import org.cartscheduler.generator.OccurrenceScoreRule;
import org.cartscheduler.generator.PairRules;

/**
 * Immutable database configuration snapshot used by one schedule-generation
 * request. Loading it once keeps every alternative proposal variant evaluated
 * against the same rules and avoids repeated database reads during search.
 */
record ScheduleGenerationConfiguration(
        RowFullOrEmptyScore rowScore,
        Map<Long, IntRange> configuredServiceRanges,
        Map<Long, IntRange> configuredBreakRanges,
        PairRules globalPairRules,
        Map<CandidatePair, Integer> pairScorePoints,
        List<OccurrenceScoreRule> candidateAssignmentScoreRules,
        Map<Long, Integer> participantPrivilegePoints,
        List<OccurrenceScoreRule> pairAssignmentScoreRules
) {
    ScheduleGenerationConfiguration {
        Objects.requireNonNull(rowScore, "rowScore must not be null");
        Objects.requireNonNull(globalPairRules, "globalPairRules must not be null");
        configuredServiceRanges = Map.copyOf(configuredServiceRanges);
        configuredBreakRanges = Map.copyOf(configuredBreakRanges);
        pairScorePoints = Map.copyOf(pairScorePoints);
        candidateAssignmentScoreRules = List.copyOf(candidateAssignmentScoreRules);
        participantPrivilegePoints = Map.copyOf(participantPrivilegePoints);
        pairAssignmentScoreRules = List.copyOf(pairAssignmentScoreRules);
    }

    boolean hasOnlyRowScore() {
        return pairScorePoints.isEmpty()
                && !OccurrenceScoreRule.hasNonZeroPoints(candidateAssignmentScoreRules)
                && participantPrivilegePoints.isEmpty()
                && !OccurrenceScoreRule.hasNonZeroPoints(pairAssignmentScoreRules);
    }
}

/** Points configured for empty and complete schedule rows. */
record RowFullOrEmptyScore(
        int emptyRowPoints,
        int fullRowAdjacentToIncompletePoints,
        int fullRowNotAdjacentToIncompletePoints
) {
}
