package org.cartscheduler.generator.postgenerationfilter;

import java.util.List;
import java.util.Map;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.PairRules;
import org.cartscheduler.generator.PostGenerationFilter;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;

/** Ensures a candidate from a required pair is never left alone in a row. */
public class PostGenerationFilterPairRules implements PostGenerationFilter {
    private final PairRules globalRules;
    private final Map<Integer, Map<Integer, PairRules>> rowRulesBySubSheet;

    public PostGenerationFilterPairRules(PairRules globalRules) {
        this(globalRules, Map.of());
    }

    public PostGenerationFilterPairRules(
            PairRules globalRules,
            Map<Integer, Map<Integer, PairRules>> rowRulesBySubSheet
    ) {
        this.globalRules = globalRules;
        this.rowRulesBySubSheet = rowRulesBySubSheet;
    }

    @Override
    public boolean isAccepted(Sheet sheet, SubSheet subSheet) {
        int subSheetIndex = sheet.getSubSheets().indexOf(subSheet);
        for (int row = 0; row < subSheet.getSizeY(); row++) {
            PairRules rowRules = rowRulesBySubSheet
                    .getOrDefault(subSheetIndex, Map.of())
                    .getOrDefault(row, PairRules.empty());
            List<Candidate> candidates = subSheet.getCells(null, row).stream()
                    .map(cell -> cell.getCandidate())
                    .filter(candidate -> candidate != null)
                    .toList();

            if (candidates.size() == 1) {
                Candidate candidate = candidates.getFirst();
                if (globalRules.requiresPartner(candidate.getId()) || rowRules.requiresPartner(candidate.getId())) {
                    return false;
                }
                continue;
            }

            for (int first = 0; first < candidates.size(); first++) {
                for (int second = first + 1; second < candidates.size(); second++) {
                    long firstCandidateId = candidates.get(first).getId();
                    long secondCandidateId = candidates.get(second).getId();
                    if (!globalRules.accepts(firstCandidateId, secondCandidateId)
                            || !rowRules.accepts(firstCandidateId, secondCandidateId)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
