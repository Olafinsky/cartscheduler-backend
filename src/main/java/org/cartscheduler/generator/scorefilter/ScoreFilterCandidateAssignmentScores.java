package org.cartscheduler.generator.scorefilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.CandidateSelectionHeuristic;
import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.GeneratorSearchContext;
import org.cartscheduler.generator.OccurrenceScoreRule;
import org.cartscheduler.generator.OptimisticScoreFilter;
import org.cartscheduler.generator.ScoreFilter;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;

/**
 * Applies configured marginal points to the first, second and subsequent
 * assignments of each candidate across the whole sheet. The contribution for
 * a sub-sheet contains only its own chronological occurrences, so summing
 * all sub-sheet scores gives one global workload-distribution score.
 */
public class ScoreFilterCandidateAssignmentScores
        implements ScoreFilter, OptimisticScoreFilter, CandidateSelectionHeuristic {
    private final List<OccurrenceScoreRule> rules;
    private final int maximumPositivePoints;

    public ScoreFilterCandidateAssignmentScores(List<OccurrenceScoreRule> rules) {
        this.rules = OccurrenceScoreRule.sortedAndValidated(rules);
        this.maximumPositivePoints = OccurrenceScoreRule.maximumPositivePoints(this.rules);
    }

    @Override
    public int calculateScore(Sheet sheet, SubSheet subSheet) {
        int targetSubSheetIndex = subSheetIndex(sheet, subSheet);
        Map<Long, Integer> assignmentCountByCandidate = new HashMap<>();
        int score = 0;

        for (int subSheetIndex = 0; subSheetIndex <= targetSubSheetIndex; subSheetIndex++) {
            for (Cell cell : sheet.getSubSheets().get(subSheetIndex).getCells()) {
                Candidate candidate = cell.getCandidate();
                if (candidate == null) {
                    continue;
                }
                int occurrence = assignmentCountByCandidate.merge(candidate.getId(), 1, Integer::sum);
                if (subSheetIndex == targetSubSheetIndex) {
                    score += OccurrenceScoreRule.pointsFor(rules, occurrence);
                }
            }
        }
        return score;
    }

    @Override
    public int maximumPossibleScore(GeneratorSearchContext context) {
        return boundedProduct(context.subSheet().getCells().size(), maximumPositivePoints);
    }

    @Override
    public int priority(GeneratorSearchContext context, Candidate candidate) {
        int previousAssignments = assignmentsBeforeNextAssignment(context, candidate);
        return OccurrenceScoreRule.pointsFor(rules, previousAssignments + 1);
    }

    private int assignmentsBeforeNextAssignment(GeneratorSearchContext context, Candidate candidate) {
        int targetSubSheetIndex = subSheetIndex(context.sheet(), context.subSheet());
        int assignments = 0;
        for (int subSheetIndex = 0; subSheetIndex <= targetSubSheetIndex; subSheetIndex++) {
            SubSheet currentSubSheet = context.sheet().getSubSheets().get(subSheetIndex);
            for (Cell cell : currentSubSheet.getCells()) {
                if (subSheetIndex == targetSubSheetIndex && context.isUnassigned(cell)) {
                    continue;
                }
                Candidate assignedCandidate = cell.getCandidate();
                if (assignedCandidate != null && assignedCandidate.getId() == candidate.getId()) {
                    assignments++;
                }
            }
        }
        return assignments;
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
