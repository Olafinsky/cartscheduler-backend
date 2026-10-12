package org.cartscheduler.generator;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Database-independent pair constraints used by the raw sheet generator.
 * Required pairs mean that a configured candidate may only share a row with
 * one of its configured partners.
 */
public final class PairRules {
    private final Set<CandidatePair> requiredPairs;
    private final Set<CandidatePair> forbiddenPairs;
    private final Map<Long, Set<Long>> requiredPartnersByCandidate;

    public PairRules(Collection<CandidatePair> requiredPairs, Collection<CandidatePair> forbiddenPairs) {
        this.requiredPairs = Set.copyOf(requiredPairs);
        this.forbiddenPairs = Set.copyOf(forbiddenPairs);
        this.requiredPartnersByCandidate = requiredPartners(this.requiredPairs);
    }

    public static PairRules empty() {
        return new PairRules(Set.of(), Set.of());
    }

    public boolean accepts(long firstCandidateId, long secondCandidateId) {
        CandidatePair pair = new CandidatePair(firstCandidateId, secondCandidateId);
        if (forbiddenPairs.contains(pair)) {
            return false;
        }
        return acceptsRequiredPartner(firstCandidateId, secondCandidateId)
                && acceptsRequiredPartner(secondCandidateId, firstCandidateId);
    }

    public boolean requiresPartner(long candidateId) {
        return requiredPartnersByCandidate.containsKey(candidateId);
    }

    private boolean acceptsRequiredPartner(long candidateId, long partnerId) {
        Set<Long> requiredPartners = requiredPartnersByCandidate.get(candidateId);
        return requiredPartners == null || requiredPartners.contains(partnerId);
    }

    private Map<Long, Set<Long>> requiredPartners(Collection<CandidatePair> pairs) {
        Map<Long, Set<Long>> result = new HashMap<>();
        for (CandidatePair pair : pairs) {
            result.computeIfAbsent(pair.firstCandidateId(), ignored -> new HashSet<>())
                    .add(pair.secondCandidateId());
            result.computeIfAbsent(pair.secondCandidateId(), ignored -> new HashSet<>())
                    .add(pair.firstCandidateId());
        }
        Map<Long, Set<Long>> immutableResult = new HashMap<>();
        result.forEach((candidateId, partners) -> immutableResult.put(candidateId, Set.copyOf(partners)));
        return Map.copyOf(immutableResult);
    }
}
