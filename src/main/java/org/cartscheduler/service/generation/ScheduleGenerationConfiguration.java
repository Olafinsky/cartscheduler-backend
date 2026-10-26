package org.cartscheduler.service.generation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.cartscheduler.generator.CandidatePair;
import org.cartscheduler.generator.IntRange;
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
        List<CandidateUniquenessScoreRule> candidateUniquenessScoreRules,
        Map<Long, Integer> participantPrivilegePoints,
        Integer pairDiversityPoints
) {
    ScheduleGenerationConfiguration {
        Objects.requireNonNull(rowScore, "rowScore must not be null");
        Objects.requireNonNull(globalPairRules, "globalPairRules must not be null");
        configuredServiceRanges = Map.copyOf(configuredServiceRanges);
        configuredBreakRanges = Map.copyOf(configuredBreakRanges);
        pairScorePoints = Map.copyOf(pairScorePoints);
        candidateUniquenessScoreRules = List.copyOf(candidateUniquenessScoreRules);
        participantPrivilegePoints = Map.copyOf(participantPrivilegePoints);
    }

    boolean hasOnlyRowScore() {
        return pairScorePoints.isEmpty()
                && candidateUniquenessScoreRules.isEmpty()
                && participantPrivilegePoints.isEmpty()
                && pairDiversityPoints == null;
    }
}

/** Points configured for empty and complete schedule rows. */
record RowFullOrEmptyScore(
        int emptyRowPoints,
        int fullRowAdjacentToIncompletePoints,
        int fullRowNotAdjacentToIncompletePoints
) {
}

/** Points awarded per occupied cell when a candidate has a matching availability count. */
record CandidateUniquenessScoreRule(int appearanceCountFrom, int appearanceCountTo, int points) {
}
