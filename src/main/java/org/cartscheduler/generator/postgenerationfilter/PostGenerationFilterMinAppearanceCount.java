package org.cartscheduler.generator.postgenerationfilter;

import org.cartscheduler.generator.PostGenerationFilter;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Cell;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import java.util.List;

public class PostGenerationFilterMinAppearanceCount implements PostGenerationFilter {

    int candidateId;
    int minAppearanceCount;

    public PostGenerationFilterMinAppearanceCount(int candidateId, int minAppearanceCount) {
        this.candidateId = candidateId;
        this.minAppearanceCount = minAppearanceCount;
    }

    @Override
    public boolean isAccepted(Sheet sheet, SubSheet subSheet) {
        List<Cell> cells = subSheet.getCells();

        return cells.stream()
            .filter(c -> c.getCandidate() != null && c.getCandidate().getId() == this.candidateId)
            .count() >= this.minAppearanceCount;
    }
}
