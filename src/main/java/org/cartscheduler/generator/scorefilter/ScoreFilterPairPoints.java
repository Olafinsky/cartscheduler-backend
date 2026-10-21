package org.cartscheduler.generator.scorefilter;

import java.util.Map;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.CandidatePair;
import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.GeneratorSearchContext;
import org.cartscheduler.generator.OptimisticScoreFilter;
import org.cartscheduler.generator.ScoreFilter;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;

/** Adds points to rows occupied by one of the configured unordered pairs. */
public class ScoreFilterPairPoints implements ScoreFilter, OptimisticScoreFilter {
    private final Map<CandidatePair, Integer> pointsByPair;

    public ScoreFilterPairPoints(Map<CandidatePair, Integer> pointsByPair) {
        this.pointsByPair = Map.copyOf(pointsByPair);
    }

    @Override
    public int calculateScore(Sheet sheet, SubSheet subSheet) {
        int score = 0;
        for (int row = 0; row < subSheet.getSizeY(); row++) {
            CandidatePair pair = pairInRow(subSheet, row);
            if (pair != null) {
                score += pointsByPair.getOrDefault(pair, 0);
            }
        }
        return score;
    }

    @Override
    public int maximumPossibleScore(GeneratorSearchContext context) {
        int maximumPairPoints = pointsByPair.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        int score = 0;
        SubSheet subSheet = context.subSheet();
        for (int row = 0; row < subSheet.getSizeY(); row++) {
            if (isRowComplete(context, subSheet, row)) {
                CandidatePair pair = pairInRow(subSheet, row);
                if (pair != null) {
                    score += pointsByPair.getOrDefault(pair, 0);
                }
            } else {
                score += Math.max(0, maximumPairPoints);
            }
        }
        return score;
    }

    static CandidatePair pairInRow(SubSheet subSheet, int row) {
        Candidate firstCandidate = null;
        Candidate secondCandidate = null;
        for (int x = 0; x < subSheet.getSizeX(); x++) {
            Candidate candidate = subSheet.getCell(x, row).getCandidate();
            if (candidate == null) {
                continue;
            }
            if (firstCandidate == null) {
                firstCandidate = candidate;
            } else if (secondCandidate == null) {
                secondCandidate = candidate;
            } else {
                return null;
            }
        }
        if (firstCandidate == null || secondCandidate == null || firstCandidate.getId() == secondCandidate.getId()) {
            return null;
        }
        return new CandidatePair(firstCandidate.getId(), secondCandidate.getId());
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
