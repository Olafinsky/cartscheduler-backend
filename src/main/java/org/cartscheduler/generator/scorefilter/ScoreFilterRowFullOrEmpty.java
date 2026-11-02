

package org.cartscheduler.generator.scorefilter;

import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.GeneratorSearchContext;
import org.cartscheduler.generator.OptimisticScoreFilter;
import org.cartscheduler.generator.ScoreFilter;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;

public class ScoreFilterRowFullOrEmpty implements ScoreFilter, OptimisticScoreFilter {
    private final int emptyRowPoints;
    private final int fullRowAdjacentToIncompletePoints;
    private final int fullRowNotAdjacentToIncompletePoints;

    public ScoreFilterRowFullOrEmpty(
            int emptyRowPoints,
            int fullRowAdjacentToIncompletePoints,
            int fullRowNotAdjacentToIncompletePoints
    ) {
        this.emptyRowPoints = emptyRowPoints;
        this.fullRowAdjacentToIncompletePoints = fullRowAdjacentToIncompletePoints;
        this.fullRowNotAdjacentToIncompletePoints = fullRowNotAdjacentToIncompletePoints;
    }

    @Override
    public int calculateScore(Sheet sheet, SubSheet subSheet) {

        int score = 0;

        for (int y = 0; y < subSheet.getSizeY(); y++) {
            int setCandidatesCount = setCandidateCount(subSheet, y);

            if (setCandidatesCount == subSheet.getSizeX()) {
                score += touchesIncompleteRow(y, subSheet)
                    ? fullRowAdjacentToIncompletePoints
                    : fullRowNotAdjacentToIncompletePoints;
            }
            else if (setCandidatesCount == 0) {
                score += emptyRowPoints;
            }
        }

        return score;
    }

    @Override
    public int maximumPossibleScore(GeneratorSearchContext context) {
        SubSheet subSheet = context.subSheet();
        int score = 0;
        int maximumFullRowPoints = Math.max(
                fullRowAdjacentToIncompletePoints,
                fullRowNotAdjacentToIncompletePoints
        );
        for (int row = 0; row < subSheet.getSizeY(); row++) {
            int occupiedCells = setCandidateCount(subSheet, row);
            int unassignedCells = unassignedCellCount(context, subSheet, row);
            if (occupiedCells == subSheet.getSizeX()) {
                score += maximumFullRowPoints;
            } else if (occupiedCells == 0 && unassignedCells == 0) {
                score += emptyRowPoints;
            } else if (occupiedCells == 0) {
                score += Math.max(emptyRowPoints, Math.max(0, maximumFullRowPoints));
            } else if (unassignedCells > 0) {
                score += Math.max(0, maximumFullRowPoints);
            }
        }
        return score;
    }

    private boolean touchesIncompleteRow(int y, SubSheet subSheet) {
        boolean previousRowIncomplete = y > 0
            && setCandidateCount(subSheet, y - 1) < subSheet.getSizeX();
        if (previousRowIncomplete) {
            return true;
        }

        return y < (subSheet.getSizeY() - 1)
            && setCandidateCount(subSheet, y + 1) < subSheet.getSizeX();
    }

    private int setCandidateCount(SubSheet subSheet, int row) {
        int count = 0;
        for (int x = 0; x < subSheet.getSizeX(); x++) {
            if (subSheet.getCell(x, row).getCandidate() != null) {
                count++;
            }
        }
        return count;
    }

    private int unassignedCellCount(GeneratorSearchContext context, SubSheet subSheet, int row) {
        int count = 0;
        for (int x = 0; x < subSheet.getSizeX(); x++) {
            Cell cell = subSheet.getCell(x, row);
            if (context.isUnassigned(cell)) {
                count++;
            }
        }
        return count;
    }
}

