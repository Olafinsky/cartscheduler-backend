
package org.cartscheduler.generator.acceptancefilter;

import org.cartscheduler.generator.AcceptanceFilter;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import java.util.List;
import java.util.stream.Collectors;

public class AcceptanceFilterGapBetweenBlocks implements AcceptanceFilter {

    Integer gapMinValue;
    Integer gapMaxValue;
    int candidateId;

    public AcceptanceFilterGapBetweenBlocks(int candidateId, int gapMinValue, int gapMaxValue) {
        this.candidateId = candidateId;
        this.gapMinValue = gapMinValue;
        this.gapMaxValue = gapMaxValue;
    }

    @Override
    public boolean isAccepted(Candidate candidate, Sheet sheet, SubSheet subSheet, Cell cell) {
        if (candidate.getId() != this.candidateId) {
            return true;
        }

        List<Integer> appearances = subSheet.getCells()
            .stream()
            .filter(c -> c.getCandidate() != null && c.getCandidate().getId() == candidateId)
            .map(c -> c.getY())
            .sorted((valueA, valueB)->valueA.compareTo(valueB))
            .collect(Collectors.toList());

        for (Integer appearance : appearances) {
            if (isDIfferenceOutOfRange(appearance, cell.getY())) {
                return false;
            }
        }
        return true;
    }

    private boolean isDIfferenceOutOfRange(Integer lastValue, int currentValue) {

        int diff = Math.abs(currentValue - lastValue) - 1;
        return diff < this.gapMinValue || diff > this.gapMaxValue;
    }
}
