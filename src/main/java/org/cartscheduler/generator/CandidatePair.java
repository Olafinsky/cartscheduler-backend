package org.cartscheduler.generator;

/**
 * An unordered pair of distinct candidates. The canonical order makes pair
 * rules and scores independent from the participant column in a row.
 */
public record CandidatePair(long firstCandidateId, long secondCandidateId) {
    public CandidatePair {
        if (firstCandidateId == secondCandidateId) {
            throw new IllegalArgumentException("A candidate pair must contain two different candidates");
        }
        if (firstCandidateId > secondCandidateId) {
            long originalFirstCandidateId = firstCandidateId;
            firstCandidateId = secondCandidateId;
            secondCandidateId = originalFirstCandidateId;
        }
    }
}
