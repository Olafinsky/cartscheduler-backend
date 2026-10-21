
package org.cartscheduler.generator.acceptancefilter;

import org.cartscheduler.generator.AcceptanceFilter;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import java.util.Map;

public class AcceptanceFilterAppearanceLimit implements AcceptanceFilter {

    private long candidateId;
    private int appearanceLimit;
    private Map<Integer, Map<Long, Integer>> appearanceLimitsBySubSheet;

    public AcceptanceFilterAppearanceLimit(long candidateId, int appearanceLimit) {
        this.candidateId = candidateId;
        this.appearanceLimit = appearanceLimit;
    }

    public AcceptanceFilterAppearanceLimit(Map<Integer, Map<Long, Integer>> appearanceLimitsBySubSheet) {
        this.appearanceLimitsBySubSheet = appearanceLimitsBySubSheet;
    }

    @Override
    public boolean isAccepted(Candidate candidate, Sheet sheet, SubSheet subSheet, Cell cell) {
        Integer limit = appearanceLimitFor(candidate, sheet, subSheet);
        if (limit == null) {
            return true;
        }

        int appearances = 0;
        for (Cell existingCell : subSheet.getCells()) {
            Candidate existingCandidate = existingCell.getCandidate();
            if (existingCandidate != null && existingCandidate.getId() == candidate.getId()) {
                appearances++;
            }
        }
        return appearances < limit;
    }

    private Integer appearanceLimitFor(Candidate candidate, Sheet sheet, SubSheet subSheet) {
        if (appearanceLimitsBySubSheet == null) {
            return candidate.getId() == candidateId ? appearanceLimit : null;
        }
        int subSheetIndex = sheet.getSubSheets().indexOf(subSheet);
        return appearanceLimitsBySubSheet
                .getOrDefault(subSheetIndex, Map.of())
                .get(candidate.getId());
    }
}
