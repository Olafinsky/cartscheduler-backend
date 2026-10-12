
package org.cartscheduler.generator.acceptancefilter;

import org.cartscheduler.generator.AcceptanceFilter;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.IntRange;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AcceptanceFilterGapBetweenBlocks implements AcceptanceFilter {

    private Integer gapMinValue;
    private Integer gapMaxValue;
    private long candidateId;
    private Map<Integer, Map<Long, IntRange>> gapRangesBySubSheet;

    public AcceptanceFilterGapBetweenBlocks(long candidateId, int gapMinValue, int gapMaxValue) {
        this.candidateId = candidateId;
        this.gapMinValue = gapMinValue;
        this.gapMaxValue = gapMaxValue;
    }

    public AcceptanceFilterGapBetweenBlocks(Map<Integer, Map<Long, IntRange>> gapRangesBySubSheet) {
        this.gapRangesBySubSheet = gapRangesBySubSheet;
    }

    @Override
    public boolean isAccepted(Candidate candidate, Sheet sheet, SubSheet subSheet, Cell cell) {
        IntRange gapRange = gapRangeFor(candidate, sheet, subSheet);
        if (gapRange == null) {
            return true;
        }

        List<Integer> appearances = subSheet.getCells()
            .stream()
            .filter(c -> c.getCandidate() != null && c.getCandidate().getId() == candidate.getId())
            .map(c -> c.getY())
            .sorted((valueA, valueB)->valueA.compareTo(valueB))
            .collect(Collectors.toList());

        for (Integer appearance : appearances) {
            if (isDifferenceOutOfRange(appearance, cell.getY(), gapRange)) {
                return false;
            }
        }
        return true;
    }

    private IntRange gapRangeFor(Candidate candidate, Sheet sheet, SubSheet subSheet) {
        if (gapRangesBySubSheet == null) {
            return candidate.getId() == candidateId ? new IntRange(gapMinValue, gapMaxValue) : null;
        }
        int subSheetIndex = sheet.getSubSheets().indexOf(subSheet);
        return gapRangesBySubSheet
                .getOrDefault(subSheetIndex, Map.of())
                .get(candidate.getId());
    }

    private boolean isDifferenceOutOfRange(Integer lastValue, int currentValue, IntRange gapRange) {

        int diff = Math.abs(currentValue - lastValue) - 1;
        return diff < gapRange.minimum() || diff > gapRange.maximum();
    }
}
