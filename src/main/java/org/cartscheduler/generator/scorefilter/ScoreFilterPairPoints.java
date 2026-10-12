package org.cartscheduler.generator.scorefilter;

import java.util.List;
import java.util.Map;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.CandidatePair;
import org.cartscheduler.generator.ScoreFilter;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;

/** Adds points to rows occupied by one of the configured unordered pairs. */
public class ScoreFilterPairPoints implements ScoreFilter {
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

    static CandidatePair pairInRow(SubSheet subSheet, int row) {
        List<Candidate> candidates = subSheet.getCells(null, row).stream()
                .map(cell -> cell.getCandidate())
                .filter(candidate -> candidate != null)
                .toList();
        if (candidates.size() != 2 || candidates.getFirst().getId() == candidates.getLast().getId()) {
            return null;
        }
        return new CandidatePair(candidates.getFirst().getId(), candidates.getLast().getId());
    }
}
