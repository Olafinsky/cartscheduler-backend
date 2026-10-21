package org.cartscheduler.generator;

import java.util.List;

/**
 * Read-only view of a partially generated sub-sheet. Optional search filters
 * use it to reject a branch before every cell has been filled.
 */
public interface GeneratorSearchContext {
    Sheet sheet();

    SubSheet subSheet();

    List<Candidate> candidates();

    /** Cells for which the generator has not selected a value yet. */
    List<Cell> unassignedCells();

    /**
     * Constant-time implementations may override this helper. The default
     * keeps existing custom contexts source-compatible.
     */
    default boolean isUnassigned(Cell cell) {
        return unassignedCells().contains(cell);
    }

    /**
     * Checks the regular acceptance filters against the current partial
     * assignment. The supplied cell must be unassigned.
     */
    boolean canAssign(Candidate candidate, Cell cell);
}
