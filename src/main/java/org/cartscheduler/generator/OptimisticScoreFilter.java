package org.cartscheduler.generator;

/**
 * A score filter able to provide a safe upper bound for a partial assignment.
 * The generator uses it for branch-and-bound pruning once it already has a
 * valid result.
 */
public interface OptimisticScoreFilter {
    int maximumPossibleScore(GeneratorSearchContext context);
}
