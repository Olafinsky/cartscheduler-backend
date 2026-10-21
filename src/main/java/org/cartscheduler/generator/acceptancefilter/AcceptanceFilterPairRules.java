package org.cartscheduler.generator.acceptancefilter;

import java.util.Map;
import org.cartscheduler.generator.AcceptanceFilter;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.PairRules;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;

/** Applies global and day/row-specific pair constraints while a cell is filled. */
public class AcceptanceFilterPairRules implements AcceptanceFilter {
    private final PairRules globalRules;
    private final Map<Integer, Map<Integer, PairRules>> rowRulesBySubSheet;

    public AcceptanceFilterPairRules(PairRules globalRules) {
        this(globalRules, Map.of());
    }

    public AcceptanceFilterPairRules(
            PairRules globalRules,
            Map<Integer, Map<Integer, PairRules>> rowRulesBySubSheet
    ) {
        this.globalRules = globalRules;
        this.rowRulesBySubSheet = rowRulesBySubSheet;
    }

    @Override
    public boolean isAccepted(Candidate candidate, Sheet sheet, SubSheet subSheet, Cell cell) {
        PairRules rowRules = rowRulesFor(sheet, subSheet, cell);
        for (int x = 0; x < subSheet.getSizeX(); x++) {
            Cell otherCell = subSheet.getCell(x, cell.getY());
            if (otherCell == cell) {
                continue;
            }
            Candidate otherCandidate = otherCell.getCandidate();
            if (otherCandidate != null
                    && (otherCandidate.getId() == candidate.getId()
                    || !globalRules.accepts(candidate.getId(), otherCandidate.getId())
                    || !rowRules.accepts(candidate.getId(), otherCandidate.getId()))) {
                return false;
            }
        }
        return true;
    }

    private PairRules rowRulesFor(Sheet sheet, SubSheet subSheet, Cell cell) {
        int subSheetIndex = sheet.getSubSheets().indexOf(subSheet);
        return rowRulesBySubSheet
                .getOrDefault(subSheetIndex, Map.of())
                .getOrDefault(cell.getY(), PairRules.empty());
    }
}
