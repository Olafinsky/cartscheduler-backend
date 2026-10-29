package org.cartscheduler.generator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Points for an ordinal occurrence within a generated sheet. For example, a
 * rule for the range {@code 2..3} applies independently to the second and
 * third occurrence of a candidate or pair.
 */
public record OccurrenceScoreRule(int occurrenceFrom, int occurrenceTo, int points) {
    public OccurrenceScoreRule {
        if (occurrenceFrom < 1) {
            throw new IllegalArgumentException("Occurrence range must start at one or more");
        }
        if (occurrenceTo < occurrenceFrom) {
            throw new IllegalArgumentException("Occurrence range end must not precede its start");
        }
    }

    public boolean appliesTo(int occurrence) {
        return occurrence >= occurrenceFrom && occurrence <= occurrenceTo;
    }

    /**
     * Produces an immutable range-ordered rule list and rejects overlapping
     * ranges. An occurrence must have one unambiguous marginal score.
     */
    public static List<OccurrenceScoreRule> sortedAndValidated(Collection<OccurrenceScoreRule> rules) {
        Objects.requireNonNull(rules, "rules must not be null");
        List<OccurrenceScoreRule> sorted = new ArrayList<>(rules);
        sorted.forEach(rule -> Objects.requireNonNull(rule, "occurrence score rule must not be null"));
        sorted.sort(Comparator.comparingInt(OccurrenceScoreRule::occurrenceFrom)
                .thenComparingInt(OccurrenceScoreRule::occurrenceTo));

        int previousEnd = 0;
        for (OccurrenceScoreRule rule : sorted) {
            if (rule.occurrenceFrom <= previousEnd) {
                throw new IllegalArgumentException("Occurrence score ranges must not overlap");
            }
            previousEnd = rule.occurrenceTo;
        }
        return List.copyOf(sorted);
    }

    public static int pointsFor(List<OccurrenceScoreRule> rules, int occurrence) {
        for (OccurrenceScoreRule rule : rules) {
            if (rule.appliesTo(occurrence)) {
                return rule.points;
            }
        }
        return 0;
    }

    public static int maximumPositivePoints(List<OccurrenceScoreRule> rules) {
        int maximum = 0;
        for (OccurrenceScoreRule rule : rules) {
            maximum = Math.max(maximum, rule.points);
        }
        return maximum;
    }

    public static boolean hasNonZeroPoints(List<OccurrenceScoreRule> rules) {
        return rules.stream().anyMatch(rule -> rule.points != 0);
    }
}
