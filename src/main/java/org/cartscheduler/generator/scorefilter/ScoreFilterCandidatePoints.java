package org.cartscheduler.generator.scorefilter;

import java.util.Map;
import org.cartscheduler.generator.ScoreFilter;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;

/** Adds configured points for every cell occupied by an individual candidate. */
public class ScoreFilterCandidatePoints implements ScoreFilter {
    private final Map<Long, Integer> pointsByCandidate;

    public ScoreFilterCandidatePoints(Map<Long, Integer> pointsByCandidate) {
        this.pointsByCandidate = Map.copyOf(pointsByCandidate);
    }

    @Override
    public int calculateScore(Sheet sheet, SubSheet subSheet) {
        return subSheet.getCells().stream()
                .filter(cell -> cell.getCandidate() != null)
                .mapToInt(cell -> pointsByCandidate.getOrDefault(cell.getCandidate().getId(), 0))
                .sum();
    }
}
