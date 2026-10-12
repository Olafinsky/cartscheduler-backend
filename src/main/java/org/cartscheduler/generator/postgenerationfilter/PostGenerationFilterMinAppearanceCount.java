package org.cartscheduler.generator.postgenerationfilter;

import org.cartscheduler.generator.PostGenerationFilter;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import java.util.List;
import java.util.Map;

public class PostGenerationFilterMinAppearanceCount implements PostGenerationFilter {

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
        List<Cell> cells = subSheet.getCells();

        return cells.stream()
            .filter(c -> c.getCandidate() != null && c.getCandidate().getId() == candidateToCheck)
            .count() >= requiredCount;
    }
}
