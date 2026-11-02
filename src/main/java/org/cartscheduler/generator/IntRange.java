package org.cartscheduler.generator;

/**
 * Inclusive integer range used by the database-independent generator filters.
 */
public record IntRange(int minimum, int maximum) {
    public IntRange {
        if (minimum < 0) {
            throw new IllegalArgumentException("minimum must not be negative");
        }
        if (maximum < minimum) {
            throw new IllegalArgumentException("maximum must be greater than or equal to minimum");
        }
    }
}
