

package org.cartscheduler.generator.scorefilter;

import org.cartscheduler.generator.ScoreFilter;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;

public class ScoreFilterRowFullOrEmpty implements ScoreFilter {
    @Override
    public int calculateScore(Sheet sheet, SubSheet subSheet) {

        int score = 0;

        for (int y = 0; y < subSheet.getSizeY(); y++) {
            long setCandidatesCount = subSheet.getCells(null, y)
                .stream()
                .filter(c -> c.getCandidate() != null)
                .count();

            if (setCandidatesCount == subSheet.getSizeX()) {
                score += touchesIncompleteRow(y, subSheet) ? 2 : 3;
            }
            else if (setCandidatesCount == 0) {
                score += 1;
            }
        }

        return score;
    }

    private boolean touchesIncompleteRow(int y, SubSheet subSheet) {
        boolean previousRowIncomplete = y > 0
            && subSheet.getCells(null, y - 1)
                .stream()
                .filter(c -> c.getCandidate() != null)
                .count() < subSheet.getSizeX();
        if (previousRowIncomplete) {
            return true;
        }

        return y < (subSheet.getSizeY() - 1)
            && subSheet.getCells(null, y + 1)
                .stream()
                .filter(c -> c.getCandidate() != null)
                .count() < subSheet.getSizeX();
    }
}

