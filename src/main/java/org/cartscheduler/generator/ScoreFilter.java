package org.cartscheduler.generator;

/**
 * Calculates the score contribution of one sub-sheet. The total score of a
 * sheet is the sum of this method for all of its sub-sheets; a filter that
 * models a sheet-wide rule must therefore return only the contribution owned
 * by the supplied sub-sheet.
 */
public interface ScoreFilter {
    int calculateScore(Sheet sheet, SubSheet subSheet);
}

