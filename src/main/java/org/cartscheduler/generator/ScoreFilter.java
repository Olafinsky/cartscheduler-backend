package org.cartscheduler.generator;

public interface ScoreFilter {
    int calculateScore(Sheet sheet, SubSheet subSheet);
}

