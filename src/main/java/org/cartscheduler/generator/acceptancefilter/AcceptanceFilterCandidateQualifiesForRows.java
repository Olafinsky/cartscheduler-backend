
package org.cartscheduler.generator.acceptancefilter;

import org.cartscheduler.generator.AcceptanceFilter;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import java.util.List;

public class AcceptanceFilterCandidateQualifiesForRows implements AcceptanceFilter {

    List<Integer> qualifiedRowNumers;
    int candidateId;

    public AcceptanceFilterCandidateQualifiesForRows(int candidateId, List<Integer> qualifiedRowNumers) {
        this.candidateId = candidateId;
        this.qualifiedRowNumers = qualifiedRowNumers;
    }

    @Override
    public boolean isAccepted(Candidate candidate, Sheet sheet, SubSheet subSheet, Cell cell) {
        if (candidate.getId() != this.candidateId) {
            return true;
        }
        return qualifiedRowNumers.contains(cell.getY());
    }
}
