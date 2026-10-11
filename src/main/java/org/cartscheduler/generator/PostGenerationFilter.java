package org.cartscheduler.generator;

public interface PostGenerationFilter {
    boolean isAccepted(Sheet sheet, SubSheet subSheet);
}
