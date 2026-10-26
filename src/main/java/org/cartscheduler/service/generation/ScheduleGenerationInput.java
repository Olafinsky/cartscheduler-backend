package org.cartscheduler.service.generation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.generator.BreakRule;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.CandidatePair;
import org.cartscheduler.generator.PairRules;
import org.cartscheduler.generator.Sheet;

/** Complete database-independent input passed from the application layer to the raw generator. */
record ScheduleGenerationInput(
        Sheet sheet,
        List<Candidate> candidates,
        Map<Long, Participant> participantsById,
        Map<Integer, Map<Long, Set<Integer>>> qualifiedRowsBySubSheet,
        Map<Integer, Map<Long, Integer>> minimumServiceLengthsBySubSheet,
        Map<Integer, Map<Long, Integer>> maximumServiceLengthsBySubSheet,
        Map<Integer, Map<Long, BreakRule>> breakRulesBySubSheet,
        Map<Integer, Map<Integer, PairRules>> rowPairRulesBySubSheet,
        PairRules globalPairRules,
        Map<Long, Integer> candidateUniquenessPoints,
        Map<Long, Integer> participantPrivilegePoints,
        Map<CandidatePair, Integer> pairScorePoints,
        Integer pairDiversityPoints
) {
}
