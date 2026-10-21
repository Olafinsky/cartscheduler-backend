package org.cartscheduler.generator;

/**
 * Optional constraint contract for a filter that needs a candidate to receive
 * more assignments in a partial schedule.
 *
 * <p>The returned value is a lower bound only. The generator uses it to place
 * mandatory candidates before it starts filling optional cells; it never uses
 * the value to reject a valid assignment.</p>
 */
public interface RequiredCandidateAssignmentFilter {
    int remainingRequiredAssignments(GeneratorSearchContext context, Candidate candidate);
}
