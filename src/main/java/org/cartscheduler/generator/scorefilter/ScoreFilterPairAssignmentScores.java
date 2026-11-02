package org.cartscheduler.generator.scorefilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cartscheduler.generator.CandidatePair;
import org.cartscheduler.generator.GeneratorSearchContext;
import org.cartscheduler.generator.OccurrenceScoreRule;
import org.cartscheduler.generator.OptimisticScoreFilter;
import org.cartscheduler.generator.ScoreFilter;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;

/**
 * Scores ordinal uses of a complete, unordered candidate pair across the
 * whole sheet. Declining marginal points therefore prefer a balanced mix of
 * partners instead of repeatedly selecting the same pair.
 */
public class ScoreFilterPairAssignmentScores implements ScoreFilter, OptimisticScoreFilter {
    private final List<OccurrenceScoreRule> rules;
    private final int maximumPositivePoints;

    public ScoreFilterPairAssignmentScores(List<OccurrenceScoreRule> rules) {
        this.rules = OccurrenceScoreRule.sortedAndValidated(rules);
        this.maximumPositivePoints = OccurrenceScoreRule.maximumPositivePoints(this.rules);
    }

    @Override
    public int calculateScore(Sheet sheet, SubSheet subSheet) {
        int targetSubSheetIndex = subSheetIndex(sheet, subSheet);
        Map<CandidatePair, Integer> assignmentCountByPair = new HashMap<>();
        int score = 0;

        for (int subSheetIndex = 0; subSheetIndex <= targetSubSheetIndex; subSheetIndex++) {
            SubSheet currentSubSheet = sheet.getSubSheets().get(subSheetIndex);
            for (int row = 0; row < currentSubSheet.getSizeY(); row++) {
                CandidatePair pair = ScoreFilterPairPoints.pairInRow(currentSubSheet, row);
                if (pair == null) {
                    continue;
                }
                int occurrence = assignmentCountByPair.merge(pair, 1, Integer::sum);
                if (subSheetIndex == targetSubSheetIndex) {
                    score += OccurrenceScoreRule.pointsFor(rules, occurrence);
                }
            }
        }
        return score;
    }

    @Override
    public int maximumPossibleScore(GeneratorSearchContext context) {
        return boundedProduct(context.subSheet().getSizeY(), maximumPositivePoints);
    }

    private int subSheetIndex(Sheet sheet, SubSheet subSheet) {
        int subSheetIndex = sheet.getSubSheets().indexOf(subSheet);
        if (subSheetIndex < 0) {
            throw new IllegalArgumentException("The scored sub-sheet must belong to the sheet");
        }
        return subSheetIndex;
    }

    private int boundedProduct(int left, int right) {
        long result = (long) left * right;
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }
}
