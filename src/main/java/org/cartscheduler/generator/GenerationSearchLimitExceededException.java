package org.cartscheduler.generator;

/** Thrown when a bounded search cannot find a valid assignment in time. */
public class GenerationSearchLimitExceededException extends IllegalStateException {
    public GenerationSearchLimitExceededException(String message) {
        super(message);
    }
}
