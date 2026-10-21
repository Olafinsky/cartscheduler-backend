package org.cartscheduler.generator.postgenerationfilter;

import java.util.Map;
import org.cartscheduler.generator.BreakRequirement;
import org.cartscheduler.generator.BreakRule;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.CandidateSelectionHeuristic;
import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.GeneratorSearchContext;
import org.cartscheduler.generator.IntRange;
import org.cartscheduler.generator.PartialGenerationFilter;
import org.cartscheduler.generator.PostGenerationFilter;
import org.cartscheduler.generator.RequiredCandidateAssignmentFilter;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;

/**
 * Checks no-break, required-break and indifferent-break preferences after a
 * candidate assignment has been produced. A break is a gap between two
 * consecutive occupied rows for the same candidate.
 */
public class PostGenerationFilterBreakRules
        implements PostGenerationFilter, PartialGenerationFilter, CandidateSelectionHeuristic,
        RequiredCandidateAssignmentFilter {
    private final Map<Integer, Map<Long, BreakRule>> breakRulesBySubSheet;

    public PostGenerationFilterBreakRules(Map<Integer, Map<Long, BreakRule>> breakRulesBySubSheet) {
        this.breakRulesBySubSheet = breakRulesBySubSheet;
    }

    @Override
    public boolean isAccepted(Sheet sheet, SubSheet subSheet) {
        int subSheetIndex = sheet.getSubSheets().indexOf(subSheet);
        for (Map.Entry<Long, BreakRule> rule : breakRulesBySubSheet
                .getOrDefault(subSheetIndex, Map.of())
                .entrySet()) {
            if (!isAccepted(subSheet, rule.getKey(), rule.getValue())) {
                return false;
            }
        }
        return true;
    }

    private boolean isAccepted(SubSheet subSheet, long candidateId, BreakRule rule) {
        int[] occupiedRows = occupiedRows(subSheet, candidateId);
        if (occupiedRows.length == 0) {
            return true;
        }

        int[] breakLengths = breakLengths(occupiedRows);
        if (rule.requirement() == BreakRequirement.NO_BREAK) {
            return breakLengths.length == 0;
        }
        if (rule.requirement() == BreakRequirement.REQUIRE_BREAK && breakLengths.length == 0) {
            return false;
        }

        IntRange allowedBreakLength = rule.allowedBreakLength();
        if (allowedBreakLength == null) {
            return true;
        }
        for (int breakLength : breakLengths) {
            if (breakLength < allowedBreakLength.minimum() || breakLength > allowedBreakLength.maximum()) {
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
        int subSheetIndex = context.sheet().getSubSheets().indexOf(context.subSheet());
        BreakRule rule = breakRulesBySubSheet
                .getOrDefault(subSheetIndex, Map.of())
                .get(candidate.getId());
        if (rule == null || rule.requirement() != BreakRequirement.REQUIRE_BREAK) {
            return 0;
        }
        int[] occupiedRows = occupiedRows(context.subSheet(), candidate.getId());
        if (occupiedRows.length == 0 || hasOnlyAllowedBreaksWithAtLeastOneBreak(occupiedRows, rule)) {
            return 0;
        }
        // The candidate already occurs, but it has no valid break yet. One
        // more occurrence is required before an optional cell is considered.
        return 1;
    }

    @Override
    public boolean isPossible(GeneratorSearchContext context) {
        int subSheetIndex = context.sheet().getSubSheets().indexOf(context.subSheet());
        for (Map.Entry<Long, BreakRule> rule : breakRulesBySubSheet
                .getOrDefault(subSheetIndex, Map.of())
                .entrySet()) {
            if (!assignedBreaksRemainValid(context, rule.getKey(), rule.getValue())) {
                return false;
            }
        }
        return true;
    }

    private boolean assignedBreaksRemainValid(
            GeneratorSearchContext context,
            long candidateId,
            BreakRule rule
    ) {
        SubSheet subSheet = context.subSheet();
        if (rule.requirement() != BreakRequirement.NO_BREAK) {
            if (rule.requirement() == BreakRequirement.REQUIRE_BREAK
                    && requiresAnotherAssignmentForBreak(subSheet, candidateId, rule)
                    && !hasPossibleAdditionalAssignment(context, candidateId)) {
                return false;
            }
            // A required/indifferent break may still be created or shortened
            // by a later assignment. Its complete validation is deliberately
            // deferred until the leaf of the search tree.
            return true;
        }

        int previousCandidateRow = -1;
        for (int row = 0; row < subSheet.getSizeY(); row++) {
            if (candidateAssignedInRow(subSheet, row, candidateId)) {
                if (previousCandidateRow >= 0) {
                    for (int gapRow = previousCandidateRow + 1; gapRow < row; gapRow++) {
                        if (isRowComplete(context, subSheet, gapRow)
                                && !candidateAssignedInRow(subSheet, gapRow, candidateId)) {
                            return false;
                        }
                    }
                }
                previousCandidateRow = row;
            }
        }
        return true;
    }

    private boolean requiresAnotherAssignmentForBreak(SubSheet subSheet, long candidateId, BreakRule rule) {
        int[] occupiedRows = occupiedRows(subSheet, candidateId);
        return occupiedRows.length > 0 && !hasOnlyAllowedBreaksWithAtLeastOneBreak(occupiedRows, rule);
    }

    private boolean hasPossibleAdditionalAssignment(GeneratorSearchContext context, long candidateId) {
        for (Candidate candidate : context.candidates()) {
            if (candidate.getId() != candidateId) {
                continue;
            }
            for (Cell cell : context.unassignedCells()) {
                if (context.canAssign(candidate, cell)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasOnlyAllowedBreaksWithAtLeastOneBreak(int[] occupiedRows, BreakRule rule) {
        int[] breakLengths = breakLengths(occupiedRows);
        if (breakLengths.length == 0) {
            return false;
        }
        IntRange allowedBreakLength = rule.allowedBreakLength();
        if (allowedBreakLength == null) {
            return true;
        }
        for (int breakLength : breakLengths) {
            if (breakLength < allowedBreakLength.minimum() || breakLength > allowedBreakLength.maximum()) {
                return false;
            }
        }
        return true;
    }

    private boolean isRowComplete(GeneratorSearchContext context, SubSheet subSheet, int row) {
        for (int x = 0; x < subSheet.getSizeX(); x++) {
            if (context.isUnassigned(subSheet.getCell(x, row))) {
                return false;
            }
        }
        return true;
    }

    private boolean candidateAssignedInRow(SubSheet subSheet, int row, long candidateId) {
        for (int x = 0; x < subSheet.getSizeX(); x++) {
            Candidate candidate = subSheet.getCell(x, row).getCandidate();
            if (candidate != null && candidate.getId() == candidateId) {
                return true;
            }
        }
        return false;
    }

    private int[] occupiedRows(SubSheet subSheet, long candidateId) {
        int[] result = new int[subSheet.getSizeY()];
        int count = 0;
        for (int row = 0; row < subSheet.getSizeY(); row++) {
            if (candidateAssignedInRow(subSheet, row, candidateId)) {
                result[count++] = row;
            }
        }
        int[] compactResult = new int[count];
        System.arraycopy(result, 0, compactResult, 0, count);
        return compactResult;
    }

    private int[] breakLengths(int[] occupiedRows) {
        int[] result = new int[Math.max(0, occupiedRows.length - 1)];
        int count = 0;
        for (int index = 1; index < occupiedRows.length; index++) {
            int length = occupiedRows[index] - occupiedRows[index - 1] - 1;
            if (length > 0) {
                result[count++] = length;
            }
        }
        int[] compactResult = new int[count];
        System.arraycopy(result, 0, compactResult, 0, count);
        return compactResult;
    }
}
