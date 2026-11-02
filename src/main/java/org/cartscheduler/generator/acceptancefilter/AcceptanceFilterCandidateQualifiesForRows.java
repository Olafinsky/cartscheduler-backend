
package org.cartscheduler.generator.acceptancefilter;

import org.cartscheduler.generator.AcceptanceFilter;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import java.util.Map;
import java.util.Set;
import java.util.List;

public class AcceptanceFilterCandidateQualifiesForRows implements AcceptanceFilter {

    private List<Integer> qualifiedRowNumbers;
    private long candidateId;
    private Map<Integer, Map<Long, Set<Integer>>> qualifiedRowsBySubSheet;

    public AcceptanceFilterCandidateQualifiesForRows(long candidateId, List<Integer> qualifiedRowNumbers) {
        this.candidateId = candidateId;
        this.qualifiedRowNumbers = qualifiedRowNumbers;
    }

    /**
     * Uses a separate availability map for every sub-sheet. It is useful when a
     * candidate has different availability on different days.
     */
    public AcceptanceFilterCandidateQualifiesForRows(
            Map<Integer, Map<Long, Set<Integer>>> qualifiedRowsBySubSheet
    ) {
        this.qualifiedRowsBySubSheet = qualifiedRowsBySubSheet;
    }

    @Override
    public boolean isAccepted(Candidate candidate, Sheet sheet, SubSheet subSheet, Cell cell) {
        if (qualifiedRowsBySubSheet != null) {
            int subSheetIndex = sheet.getSubSheets().indexOf(subSheet);
            return qualifiedRowsBySubSheet
                    .getOrDefault(subSheetIndex, Map.of())
                    .getOrDefault(candidate.getId(), Set.of())
                    .contains(cell.getY());
        }
        if (candidate.getId() != this.candidateId) {
            return true;
        }
        return qualifiedRowNumbers.contains(cell.getY());
    }
}
