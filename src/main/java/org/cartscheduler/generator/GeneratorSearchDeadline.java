package org.cartscheduler.generator;

import java.time.Duration;
import java.util.Objects;

/**
 * Monotonic deadline shared by one or more generator instances. It lets a
 * caller bound a complete schedule search without coupling the raw generator
 * to application configuration or persistence.
 */
public final class GeneratorSearchDeadline {
    private static final GeneratorSearchDeadline UNLIMITED = new GeneratorSearchDeadline(Long.MAX_VALUE);

    private final long deadlineNanos;

    private GeneratorSearchDeadline(long deadlineNanos) {
        this.deadlineNanos = deadlineNanos;
    }

    public static GeneratorSearchDeadline unlimited() {
        return UNLIMITED;
    }

    public static GeneratorSearchDeadline after(Duration duration) {
        Objects.requireNonNull(duration, "duration must not be null");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative");
        }
        return new GeneratorSearchDeadline(System.nanoTime() + duration.toNanos());
    }

    public boolean hasExpired() {
        return deadlineNanos != Long.MAX_VALUE && System.nanoTime() - deadlineNanos >= 0;
    }

    /**
     * Returns a deadline that consumes at most an equal share of the time
     * still available in this deadline. It is useful when independent
     * sub-problems should all receive a chance to improve their result.
     */
    public GeneratorSearchDeadline equalShareOfRemainingTime(int remainingParts) {
        if (remainingParts < 1) {
            throw new IllegalArgumentException("remainingParts must be at least one");
        }
        if (deadlineNanos == Long.MAX_VALUE) {
            return unlimited();
        }

        long now = System.nanoTime();
        long remainingNanos = deadlineNanos - now;
        if (remainingNanos <= 0) {
            return new GeneratorSearchDeadline(now);
        }
        long shareNanos = Math.max(1L, remainingNanos / remainingParts);
        return new GeneratorSearchDeadline(now + shareNanos);
    }
}
