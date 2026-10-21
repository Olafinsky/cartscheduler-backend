package org.cartscheduler.generator.postgenerationfilter;

import org.cartscheduler.generator.PostGenerationFilter;
import org.cartscheduler.generator.CandidateSelectionHeuristic;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.GeneratorSearchContext;
import org.cartscheduler.generator.PartialGenerationFilter;
import org.cartscheduler.generator.RequiredCandidateAssignmentFilter;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import java.util.Map;

public class PostGenerationFilterMinAppearanceCount
        implements PostGenerationFilter, PartialGenerationFilter, CandidateSelectionHeuristic,
        RequiredCandidateAssignmentFilter {

    private long candidateId;
    private int minAppearanceCount;
    private Map<Integer, Map<Long, Integer>> minAppearanceCountsBySubSheet;

    public PostGenerationFilterMinAppearanceCount(long candidateId, int minAppearanceCount) {
        this.candidateId = candidateId;
        this.minAppearanceCount = minAppearanceCount;
    }

    public PostGenerationFilterMinAppearanceCount(
            Map<Integer, Map<Long, Integer>> minAppearanceCountsBySubSheet
    ) {
        this.minAppearanceCountsBySubSheet = minAppearanceCountsBySubSheet;
    }

    @Override
    public boolean isAccepted(Sheet sheet, SubSheet subSheet) {
        long candidateToCheck = candidateId;
        int requiredCount = minAppearanceCount;
        if (minAppearanceCountsBySubSheet != null) {
            int subSheetIndex = sheet.getSubSheets().indexOf(subSheet);
            Map<Long, Integer> minimums = minAppearanceCountsBySubSheet
                    .getOrDefault(subSheetIndex, Map.of());
            for (Map.Entry<Long, Integer> minimum : minimums.entrySet()) {
                if (!hasMinimumAppearanceCount(subSheet, minimum.getKey(), minimum.getValue())) {
                    return false;
                }
            }
            return true;
        }

        return hasMinimumAppearanceCount(subSheet, candidateToCheck, requiredCount);
    }

    private boolean hasMinimumAppearanceCount(SubSheet subSheet, long candidateToCheck, int requiredCount) {
        return appearanceCount(subSheet, candidateToCheck) >= requiredCount;
    }

    @Override
    public int priority(GeneratorSearchContext context, Candidate candidate) {
        return remainingRequiredAssignments(context, candidate);
    }

    @Override
    public int remainingRequiredAssignments(GeneratorSearchContext context, Candidate candidate) {
        int requiredCount = requiredCountFor(context, candidate.getId());
        return Math.max(0, requiredCount - appearanceCount(context.subSheet(), candidate.getId()));
    }

    private int requiredCountFor(GeneratorSearchContext context, long candidateToCheck) {
        if (minAppearanceCountsBySubSheet == null) {
            return candidateToCheck == candidateId ? minAppearanceCount : 0;
        }
        int subSheetIndex = context.sheet().getSubSheets().indexOf(context.subSheet());
        return minAppearanceCountsBySubSheet
                .getOrDefault(subSheetIndex, Map.of())
                .getOrDefault(candidateToCheck, 0);
    }

    @Override
    public boolean isPossible(GeneratorSearchContext context) {
        if (minAppearanceCountsBySubSheet == null) {
            return canStillReachMinimum(context, candidateId, minAppearanceCount);
        }

        int subSheetIndex = context.sheet().getSubSheets().indexOf(context.subSheet());
        for (Map.Entry<Long, Integer> minimum : minAppearanceCountsBySubSheet
                .getOrDefault(subSheetIndex, Map.of())
                .entrySet()) {
            if (!canStillReachMinimum(context, minimum.getKey(), minimum.getValue())) {
                return false;
            }
        }
        return true;
    }

    private boolean canStillReachMinimum(
            GeneratorSearchContext context,
            long candidateToCheck,
            int requiredCount
    ) {
        int count = appearanceCount(context.subSheet(), candidateToCheck);
        if (count >= requiredCount) {
            return true;
        }

        int availableCells = 0;
        for (Cell cell : context.unassignedCells()) {
            if (canCandidateUseCell(context, candidateToCheck, cell)) {
                availableCells++;
            }
        }
        return count + availableCells >= requiredCount;
    }

    private boolean canCandidateUseCell(GeneratorSearchContext context, long candidateToCheck, Cell cell) {
        for (Candidate candidate : context.candidates()) {
            if (candidate.getId() == candidateToCheck && context.canAssign(candidate, cell)) {
                return true;
            }
        }
        return false;
    }

    private int appearanceCount(SubSheet subSheet, long candidateToCheck) {
        int count = 0;
        for (Cell cell : subSheet.getCells()) {
            Candidate candidate = cell.getCandidate();
            if (candidate != null && candidate.getId() == candidateToCheck) {
                count++;
            }
        }
        return count;
    }
}
