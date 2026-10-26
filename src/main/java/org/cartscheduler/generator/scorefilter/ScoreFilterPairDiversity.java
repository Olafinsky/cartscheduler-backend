package org.cartscheduler.generator.scorefilter;

import java.util.HashSet;
import java.util.Set;
import org.cartscheduler.generator.CandidatePair;
import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.GeneratorSearchContext;
import org.cartscheduler.generator.OptimisticScoreFilter;
import org.cartscheduler.generator.ScoreFilter;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;

/**
 * Rewards each unordered pair once across the whole sheet.
 *
 * <p>The {@link ScoreFilter} contract is evaluated once per sub-sheet, so this
 * filter returns only the points whose first occurrence belongs to the given
 * sub-sheet. Summing scores for all days therefore awards a pair exactly once
 * instead of once for every generated day.</p>
 */
public class ScoreFilterPairDiversity implements ScoreFilter, OptimisticScoreFilter {
    private final int pointsPerUniquePair;

    public ScoreFilterPairDiversity(int pointsPerUniquePair) {
        this.pointsPerUniquePair = pointsPerUniquePair;
    }

    @Override
    public int calculateScore(Sheet sheet, SubSheet subSheet) {
        Set<CandidatePair> uniquePairs = new HashSet<>();
        int score = 0;
        int targetSubSheetIndex = subSheetIndex(sheet, subSheet);
        for (int subSheetIndex = 0; subSheetIndex <= targetSubSheetIndex; subSheetIndex++) {
            SubSheet currentSubSheet = sheet.getSubSheets().get(subSheetIndex);
            for (int row = 0; row < currentSubSheet.getSizeY(); row++) {
                CandidatePair pair = ScoreFilterPairPoints.pairInRow(currentSubSheet, row);
                if (pair != null && uniquePairs.add(pair) && subSheetIndex == targetSubSheetIndex) {
                    score += pointsPerUniquePair;
                }
            }
        }
        return score;
    }

    @Override
    public int maximumPossibleScore(GeneratorSearchContext context) {
        if (pointsPerUniquePair <= 0) {
            // Empty rows and repeated pairs can always avoid an additional
            // negative diversity score, so zero remains a safe upper bound.
            return 0;
        }

        int score = 0;
        Set<CandidatePair> pairsSeenBeforeCurrentDay = pairsBefore(
                context.sheet(),
                subSheetIndex(context.sheet(), context.subSheet())
        );
        Set<CandidatePair> pairsSeenInCurrentDay = new HashSet<>();
        SubSheet subSheet = context.subSheet();
        for (int row = 0; row < subSheet.getSizeY(); row++) {
            if (!isRowComplete(context, subSheet, row)) {
                score += pointsPerUniquePair;
                continue;
            }
            CandidatePair pair = ScoreFilterPairPoints.pairInRow(subSheet, row);
            if (pair != null
                    && !pairsSeenBeforeCurrentDay.contains(pair)
                    && pairsSeenInCurrentDay.add(pair)) {
                score += pointsPerUniquePair;
            }
        }
        return score;
    }

    private Set<CandidatePair> pairsBefore(Sheet sheet, int targetSubSheetIndex) {
        Set<CandidatePair> result = new HashSet<>();
        for (int subSheetIndex = 0; subSheetIndex < targetSubSheetIndex; subSheetIndex++) {
            SubSheet currentSubSheet = sheet.getSubSheets().get(subSheetIndex);
            for (int row = 0; row < currentSubSheet.getSizeY(); row++) {
                CandidatePair pair = ScoreFilterPairPoints.pairInRow(currentSubSheet, row);
                if (pair != null) {
                    result.add(pair);
                }
            }
        }
        return result;
    }

    private int subSheetIndex(Sheet sheet, SubSheet subSheet) {
        int subSheetIndex = sheet.getSubSheets().indexOf(subSheet);
        if (subSheetIndex < 0) {
            throw new IllegalArgumentException("The scored sub-sheet must belong to the sheet");
        }
        return subSheetIndex;
    }

    private boolean isRowComplete(GeneratorSearchContext context, SubSheet subSheet, int row) {
        for (int x = 0; x < subSheet.getSizeX(); x++) {
            Cell cell = subSheet.getCell(x, row);
            if (context.isUnassigned(cell)) {
                return false;
            }
        }
        return true;
    }
}
