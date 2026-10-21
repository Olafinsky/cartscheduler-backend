package org.cartscheduler.generator;

import java.util.Objects;

/**
 * A break preference and, if a break occurs, its inclusive permitted length.
 */
public record BreakRule(BreakRequirement requirement, IntRange allowedBreakLength) {
    public BreakRule {
        Objects.requireNonNull(requirement, "requirement must not be null");
        if (requirement == BreakRequirement.NO_BREAK && allowedBreakLength != null) {
            throw new IllegalArgumentException("A no-break rule cannot have a break-length range");
        }
        if (requirement == BreakRequirement.REQUIRE_BREAK && allowedBreakLength == null) {
            throw new IllegalArgumentException("A required-break rule needs a break-length range");
        }
    }

    public BreakRule withAllowedBreakLength(IntRange breakLength) {
        return new BreakRule(requirement, breakLength);
    }
}
