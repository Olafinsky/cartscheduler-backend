package org.cartscheduler.generator;

/**
 * Optional extension for a post-generation filter.
 *
 * <p>Implementations must return {@code true} whenever a valid completed
 * assignment may still exist. Returning {@code false} lets the generator stop
 * exploring the current branch early.</p>
 */
public interface PartialGenerationFilter {
    boolean isPossible(GeneratorSearchContext context);
}
