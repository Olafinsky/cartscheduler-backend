
package org.cartscheduler.generator.acceptancefilter;

import org.cartscheduler.generator.AcceptanceFilter;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import java.util.List;

public class AcceptanceFilterAppearanceLimit implements AcceptanceFilter {

    int candidateId;
    int appearanceLimit;

    public AcceptanceFilterAppearanceLimit(int candidateId, int appearanceLimit) {
        this.candidateId = candidateId;
        this.appearanceLimit = appearanceLimit;
    }

    @Override
    public boolean isAccepted(Candidate candidate, Sheet sheet, SubSheet subSheet, Cell cell) {
        if (candidate.getId() != this.candidateId) {
            return true;
        }

        List<Cell> cells = subSheet.getCells();

        return cells.stream()
            .filter(c -> c.getCandidate() != null && c.getCandidate().getId() == this.candidateId)
            .count() < this.appearanceLimit;
    }
}
