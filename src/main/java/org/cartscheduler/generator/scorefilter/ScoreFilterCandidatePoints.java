package org.cartscheduler.generator.scorefilter;

import java.util.Map;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.GeneratorSearchContext;
import org.cartscheduler.generator.OptimisticScoreFilter;
import org.cartscheduler.generator.ScoreFilter;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;

/** Adds configured points for every cell occupied by an individual candidate. */
public class ScoreFilterCandidatePoints implements ScoreFilter, OptimisticScoreFilter {
    private final Map<Long, Integer> pointsByCandidate;

    public ScoreFilterCandidatePoints(Map<Long, Integer> pointsByCandidate) {
        this.pointsByCandidate = Map.copyOf(pointsByCandidate);
    }

    @Override
    public int calculateScore(Sheet sheet, SubSheet subSheet) {
        int score = 0;
        for (Cell cell : subSheet.getCells()) {
            Candidate candidate = cell.getCandidate();
            if (candidate != null) {
                score += pointsByCandidate.getOrDefault(candidate.getId(), 0);
            }
        }
        return score;
    }

    @Override
    public int maximumPossibleScore(GeneratorSearchContext context) {
        int maximumCandidatePoints = pointsByCandidate.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        int score = 0;
        for (Cell cell : context.subSheet().getCells()) {
            Candidate candidate = cell.getCandidate();
            if (candidate != null) {
                score += pointsByCandidate.getOrDefault(candidate.getId(), 0);
            } else if (context.isUnassigned(cell)) {
                score += Math.max(0, maximumCandidatePoints);
            }
        }
        return score;
    }
}
