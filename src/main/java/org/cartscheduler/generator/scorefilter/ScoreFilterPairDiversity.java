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

/** Rewards each unordered pair once, across all days represented by a sheet. */
public class ScoreFilterPairDiversity implements ScoreFilter, OptimisticScoreFilter {
    private final int pointsPerUniquePair;

    public ScoreFilterPairDiversity(int pointsPerUniquePair) {
        this.pointsPerUniquePair = pointsPerUniquePair;
    }

    @Override
    public int calculateScore(Sheet sheet, SubSheet subSheet) {
        Set<CandidatePair> uniquePairs = new HashSet<>();
        for (SubSheet currentSubSheet : sheet.getSubSheets()) {
            for (int row = 0; row < currentSubSheet.getSizeY(); row++) {
                CandidatePair pair = ScoreFilterPairPoints.pairInRow(currentSubSheet, row);
                if (pair != null) {
                    uniquePairs.add(pair);
                }
            }
        }
        return uniquePairs.size() * pointsPerUniquePair;
    }

    @Override
    public int maximumPossibleScore(GeneratorSearchContext context) {
        Set<CandidatePair> knownPairs = new HashSet<>();
        int potentiallyChangeableRows = 0;
        for (SubSheet subSheet : context.sheet().getSubSheets()) {
            boolean isCurrentSubSheet = subSheet == context.subSheet();
            for (int row = 0; row < subSheet.getSizeY(); row++) {
                CandidatePair pair = ScoreFilterPairPoints.pairInRow(subSheet, row);
                boolean rowComplete = !isCurrentSubSheet || isRowComplete(context, subSheet, row);
                if (pair != null && rowComplete) {
                    knownPairs.add(pair);
                } else if (!rowComplete || !isCurrentSubSheet) {
                    potentiallyChangeableRows++;
                }
            }
        }

        if (pointsPerUniquePair < 0) {
            return knownPairs.size() * pointsPerUniquePair;
        }
        return (knownPairs.size() + potentiallyChangeableRows) * pointsPerUniquePair;
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
