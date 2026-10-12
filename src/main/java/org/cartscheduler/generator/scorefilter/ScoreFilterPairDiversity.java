package org.cartscheduler.generator.scorefilter;

import java.util.HashSet;
import java.util.Set;
import org.cartscheduler.generator.CandidatePair;
import org.cartscheduler.generator.ScoreFilter;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;

/** Rewards each unordered pair once, across all days represented by a sheet. */
public class ScoreFilterPairDiversity implements ScoreFilter {
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
}
