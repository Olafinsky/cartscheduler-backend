package org.cartscheduler.generator.postgenerationfilter;

import java.util.Map;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.CandidateSelectionHeuristic;
import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.GeneratorSearchContext;
import org.cartscheduler.generator.PartialGenerationFilter;
import org.cartscheduler.generator.PostGenerationFilter;
import org.cartscheduler.generator.RequiredCandidateAssignmentFilter;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;

/**
 * Requires a minimum appearance count only after a candidate has been chosen
 * at least once. It models an optional proposal with a minimum service length:
 * the participant may be omitted entirely, but a started assignment cannot be
 * shorter than the configured minimum.
 */
public class PostGenerationFilterMinimumAppearanceWhenAssigned
        implements PostGenerationFilter, PartialGenerationFilter, CandidateSelectionHeuristic,
        RequiredCandidateAssignmentFilter {

    private final Map<Integer, Map<Long, Integer>> minimumAppearanceCountsBySubSheet;

    public PostGenerationFilterMinimumAppearanceWhenAssigned(
            Map<Integer, Map<Long, Integer>> minimumAppearanceCountsBySubSheet
    ) {
        this.minimumAppearanceCountsBySubSheet = minimumAppearanceCountsBySubSheet;
    }

    @Override
    public boolean isAccepted(Sheet sheet, SubSheet subSheet) {
        int subSheetIndex = sheet.getSubSheets().indexOf(subSheet);
        for (Map.Entry<Long, Integer> minimum : minimumAppearanceCountsBySubSheet
                .getOrDefault(subSheetIndex, Map.of())
                .entrySet()) {
            int appearances = appearanceCount(subSheet, minimum.getKey());
            if (appearances > 0 && appearances < minimum.getValue()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int priority(GeneratorSearchContext context, Candidate candidate) {
        return remainingRequiredAssignments(context, candidate);
    }

    @Override
    public int remainingRequiredAssignments(GeneratorSearchContext context, Candidate candidate) {
        int appearances = appearanceCount(context.subSheet(), candidate.getId());
        if (appearances == 0) {
            return 0;
        }
        return Math.max(0, requiredCountFor(context, candidate.getId()) - appearances);
    }

    @Override
    public boolean isPossible(GeneratorSearchContext context) {
        int subSheetIndex = context.sheet().getSubSheets().indexOf(context.subSheet());
        for (Map.Entry<Long, Integer> minimum : minimumAppearanceCountsBySubSheet
                .getOrDefault(subSheetIndex, Map.of())
                .entrySet()) {
            if (!canStillReachMinimumAfterAssignment(context, minimum.getKey(), minimum.getValue())) {
                return false;
            }
        }
        return true;
    }

    private boolean canStillReachMinimumAfterAssignment(
            GeneratorSearchContext context,
            long candidateId,
            int requiredCount
    ) {
        int appearances = appearanceCount(context.subSheet(), candidateId);
        if (appearances == 0 || appearances >= requiredCount) {
            return true;
        }

        int availableCells = 0;
        for (Cell cell : context.unassignedCells()) {
            if (canCandidateUseCell(context, candidateId, cell)) {
                availableCells++;
            }
        }
        return appearances + availableCells >= requiredCount;
    }

    private int requiredCountFor(GeneratorSearchContext context, long candidateId) {
        int subSheetIndex = context.sheet().getSubSheets().indexOf(context.subSheet());
        return minimumAppearanceCountsBySubSheet
                .getOrDefault(subSheetIndex, Map.of())
                .getOrDefault(candidateId, 0);
    }

    private boolean canCandidateUseCell(GeneratorSearchContext context, long candidateId, Cell cell) {
        for (Candidate candidate : context.candidates()) {
            if (candidate.getId() == candidateId && context.canAssign(candidate, cell)) {
                return true;
            }
        }
        return false;
    }

    private int appearanceCount(SubSheet subSheet, long candidateId) {
        int count = 0;
        for (Cell cell : subSheet.getCells()) {
            Candidate candidate = cell.getCandidate();
            if (candidate != null && candidate.getId() == candidateId) {
                count++;
            }
        }
        return count;
    }
}
