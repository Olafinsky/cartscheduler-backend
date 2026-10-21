package org.cartscheduler.generator.postgenerationfilter;

import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.GeneratorSearchContext;
import org.cartscheduler.generator.PartialGenerationFilter;
import org.cartscheduler.generator.PostGenerationFilter;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;

/**
 * Accepts only rows that are either completely empty or completely filled.
 * For the schedule generator's two columns this prevents a row with exactly
 * one participant.
 */
public class PostGenerationFilterRowsAreFullOrEmpty implements PostGenerationFilter, PartialGenerationFilter {

    @Override
    public boolean isAccepted(Sheet sheet, SubSheet subSheet) {
        for (int row = 0; row < subSheet.getSizeY(); row++) {
            int assignedCells = assignedCellCount(subSheet, row);
            if (assignedCells != 0 && assignedCells != subSheet.getSizeX()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isPossible(GeneratorSearchContext context) {
        SubSheet subSheet = context.subSheet();
        for (int row = 0; row < subSheet.getSizeY(); row++) {
            int assignedCells = assignedCellCount(subSheet, row);
            if (assignedCells == 0 || assignedCells == subSheet.getSizeX()) {
                continue;
            }
            if (!hasUnassignedCell(context, subSheet, row)) {
                return false;
            }
        }
        return true;
    }

    private int assignedCellCount(SubSheet subSheet, int row) {
        int count = 0;
        for (int column = 0; column < subSheet.getSizeX(); column++) {
            Candidate candidate = subSheet.getCell(column, row).getCandidate();
            if (candidate != null) {
                count++;
            }
        }
        return count;
    }

    private boolean hasUnassignedCell(GeneratorSearchContext context, SubSheet subSheet, int row) {
        for (int column = 0; column < subSheet.getSizeX(); column++) {
            Cell cell = subSheet.getCell(column, row);
            if (context.isUnassigned(cell)) {
                return true;
            }
        }
        return false;
    }
}
