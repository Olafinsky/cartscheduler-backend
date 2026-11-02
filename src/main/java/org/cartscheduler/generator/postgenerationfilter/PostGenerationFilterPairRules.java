package org.cartscheduler.generator.postgenerationfilter;

import java.util.Map;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.GeneratorSearchContext;
import org.cartscheduler.generator.PairRules;
import org.cartscheduler.generator.PartialGenerationFilter;
import org.cartscheduler.generator.PostGenerationFilter;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;

/** Ensures a candidate from a required pair is never left alone in a row. */
public class PostGenerationFilterPairRules implements PostGenerationFilter, PartialGenerationFilter {
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
            Candidate[] candidates = candidatesInRow(subSheet, row);
            int candidateCount = candidates.length;
            if (candidateCount == 1) {
                Candidate candidate = candidates[0];
                if (globalRules.requiresPartner(candidate.getId()) || rowRules.requiresPartner(candidate.getId())) {
                    return false;
                }
                continue;
            }

            for (int first = 0; first < candidateCount; first++) {
                for (int second = first + 1; second < candidateCount; second++) {
                    long firstCandidateId = candidates[first].getId();
                    long secondCandidateId = candidates[second].getId();
                    if (firstCandidateId == secondCandidateId) {
                        return false;
                    }
                    if (!globalRules.accepts(firstCandidateId, secondCandidateId)
                            || !rowRules.accepts(firstCandidateId, secondCandidateId)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean isPossible(GeneratorSearchContext context) {
        Sheet sheet = context.sheet();
        SubSheet subSheet = context.subSheet();
        int subSheetIndex = sheet.getSubSheets().indexOf(subSheet);
        for (int row = 0; row < subSheet.getSizeY(); row++) {
            PairRules rowRules = rowRulesBySubSheet
                    .getOrDefault(subSheetIndex, Map.of())
                    .getOrDefault(row, PairRules.empty());
            Candidate[] candidates = candidatesInRow(subSheet, row);
            if (!containsOnlyAllowedPairs(candidates, rowRules)) {
                return false;
            }

            if (candidates.length == 1) {
                Candidate candidate = candidates[0];
                if (requiresPartner(candidate, rowRules)
                        && !hasPossiblePartner(context, subSheet, row, candidate, rowRules)) {
                    return false;
                }
            }
        }
        return true;
    }

    private Candidate[] candidatesInRow(SubSheet subSheet, int row) {
        Candidate[] result = new Candidate[subSheet.getSizeX()];
        int count = 0;
        for (int x = 0; x < subSheet.getSizeX(); x++) {
            Candidate candidate = subSheet.getCell(x, row).getCandidate();
            if (candidate != null) {
                result[count++] = candidate;
            }
        }
        Candidate[] compactResult = new Candidate[count];
        System.arraycopy(result, 0, compactResult, 0, count);
        return compactResult;
    }

    private boolean containsOnlyAllowedPairs(Candidate[] candidates, PairRules rowRules) {
        for (int first = 0; first < candidates.length; first++) {
            for (int second = first + 1; second < candidates.length; second++) {
                long firstCandidateId = candidates[first].getId();
                long secondCandidateId = candidates[second].getId();
                if (firstCandidateId == secondCandidateId
                        || !globalRules.accepts(firstCandidateId, secondCandidateId)
                        || !rowRules.accepts(firstCandidateId, secondCandidateId)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean requiresPartner(Candidate candidate, PairRules rowRules) {
        return globalRules.requiresPartner(candidate.getId()) || rowRules.requiresPartner(candidate.getId());
    }

    private boolean hasPossiblePartner(
            GeneratorSearchContext context,
            SubSheet subSheet,
            int row,
            Candidate candidate,
            PairRules rowRules
    ) {
        for (int x = 0; x < subSheet.getSizeX(); x++) {
            Cell cell = subSheet.getCell(x, row);
            if (!context.isUnassigned(cell)) {
                continue;
            }
            for (Candidate possiblePartner : context.candidates()) {
                if (possiblePartner.getId() == candidate.getId()) {
                    continue;
                }
                if (globalRules.accepts(candidate.getId(), possiblePartner.getId())
                        && rowRules.accepts(candidate.getId(), possiblePartner.getId())
                        && context.canAssign(possiblePartner, cell)) {
                    return true;
                }
            }
        }
        return false;
    }
}
