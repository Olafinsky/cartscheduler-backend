package org.cartscheduler.generator;

/**
 * Optional, non-binding preference used to choose the next candidate during
 * search. A larger value means that the candidate is tried sooner; it must
 * never be used to reject an otherwise valid assignment.
 */
public interface CandidateSelectionHeuristic {
    int priority(GeneratorSearchContext context, Candidate candidate);
}
