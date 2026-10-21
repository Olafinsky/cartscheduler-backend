
package org.cartscheduler.generator.acceptancefilter;

import org.cartscheduler.generator.AcceptanceFilter;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;

public class AcceptanceFilterUniqueInRow implements AcceptanceFilter {
    @Override
    public boolean isAccepted(Candidate candidate, Sheet sheet, SubSheet subSheet, Cell cell) {
        for (int x = 0; x < subSheet.getSizeX(); x++) {
            Candidate existingCandidate = subSheet.getCell(x, cell.getY()).getCandidate();
            if (existingCandidate != null && existingCandidate.getId() == candidate.getId()) {
                return false;
            }
        }
        return true;
    }
}
