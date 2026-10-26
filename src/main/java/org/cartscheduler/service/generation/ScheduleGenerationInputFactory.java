package org.cartscheduler.service.generation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cartscheduler.entity.BreakMode;
import org.cartscheduler.entity.EnforcedParticipant;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.Proposal;
import org.cartscheduler.generator.BreakRequirement;
import org.cartscheduler.generator.BreakRule;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.CandidatePair;
import org.cartscheduler.generator.IntRange;
import org.cartscheduler.generator.PairRules;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import org.springframework.stereotype.Component;

/**
 * Translates database entities and one selected proposal variant into the
 * raw, persistence-independent generator model. It deliberately performs no
 * database access so alternative variants can be evaluated cheaply.
 */
@Component
class ScheduleGenerationInputFactory {

    ScheduleGenerationInput create(
            List<ScheduleGenerationDay> days,
            List<Proposal> proposals,
            Map<String, EnforcedParticipant> enforcedByDayName,
            ScheduleGenerationConfiguration configuration
    ) {
        return createInput(
                days,
                proposalsWithAvailableRequestedPairs(proposals),
                enforcedByDayName,
                configuration
        );
    }

    /**
     * A requested pair is meaningful only when the requested participant has
     * submitted a proposal for at least one common row on the same day. If
     * that is not the case, the source proposal is ignored rather than making
     * the whole schedule impossible to generate. The loop also handles a
     * chain of requests, e.g. A requests B and B requests an unavailable C.
     */
    private List<Proposal> proposalsWithAvailableRequestedPairs(List<Proposal> proposals) {
        List<Proposal> remaining = new ArrayList<>(proposals);
        boolean removed;
        do {
            List<Proposal> snapshot = List.copyOf(remaining);
            remaining = snapshot.stream()
                    .filter(proposal -> proposal.getPairParticipant() == null
                            || hasCompatibleRequestedPairProposal(proposal, snapshot))
                    .toList();
            removed = remaining.size() != snapshot.size();
        } while (removed);
        return remaining;
    }

    private boolean hasCompatibleRequestedPairProposal(Proposal sourceProposal, List<Proposal> proposals) {
        long requestedParticipantId = sourceProposal.getPairParticipant().getId();
        long dayId = sourceProposal.getScheduleDay().getId();
        for (Proposal candidateProposal : proposals) {
            if (candidateProposal.getParticipant().getId() != requestedParticipantId
                    || candidateProposal.getScheduleDay().getId() != dayId) {
                continue;
            }
            if (proposalsOverlap(sourceProposal, candidateProposal)) {
                return true;
            }
        }
        return false;
    }

    private boolean proposalsOverlap(Proposal first, Proposal second) {
        return first.getHourStart() <= second.getHourEnd()
                && second.getHourStart() <= first.getHourEnd();
    }

    private ScheduleGenerationInput createInput(
            List<ScheduleGenerationDay> days,
            List<Proposal> proposals,
            Map<String, EnforcedParticipant> enforcedByDayName,
            ScheduleGenerationConfiguration configuration
    ) {
        Map<Long, Integer> dayIndexesById = new HashMap<>();
        for (ScheduleGenerationDay day : days) {
            dayIndexesById.put(day.day().getId(), day.index());
        }

        Map<Long, Participant> participantsById = new HashMap<>();
        Map<Integer, Map<Long, Set<Integer>>> qualifiedRowsBySubSheet = new HashMap<>();
        Map<DayCandidateKey, IntRange> proposalServiceRanges = new HashMap<>();
        Map<DayCandidateKey, BreakRule> proposalBreakRules = new HashMap<>();
        Map<Integer, Map<Integer, Set<CandidatePair>>> proposalPairRequirements = new HashMap<>();

        for (Proposal proposal : proposals) {
            Integer dayIndex = dayIndexesById.get(proposal.getScheduleDay().getId());
            if (dayIndex == null) {
                throw new ScheduleGenerationException("Proposal " + proposal.getId() + " belongs to another schedule");
            }

            Participant participant = proposal.getParticipant();
            participantsById.put(participant.getId(), participant);
            if (proposal.getPairParticipant() != null) {
                participantsById.put(proposal.getPairParticipant().getId(), proposal.getPairParticipant());
            }

            ScheduleGenerationDay day = days.get(dayIndex);
            Set<Integer> qualifiedRows = qualifiedRows(day, proposal);
            if (qualifiedRows.isEmpty()) {
                continue;
            }
            qualifiedRowsBySubSheet
                    .computeIfAbsent(dayIndex, ignored -> new HashMap<>())
                    .computeIfAbsent(participant.getId(), ignored -> new HashSet<>())
                    .addAll(qualifiedRows);

            DayCandidateKey key = new DayCandidateKey(dayIndex, participant.getId());
            if (hasCompleteRange(proposal.getServiceLengthStart(), proposal.getServiceLengthEnd(), "service length", proposal)) {
                mergeRange(
                        proposalServiceRanges,
                        key,
                        range(proposal.getServiceLengthStart(), proposal.getServiceLengthEnd(), "proposal service length")
                );
            }
            mergeBreakRule(proposalBreakRules, key, breakRuleForProposal(proposal));

            if (proposal.getPairParticipant() != null) {
                long pairParticipantId = proposal.getPairParticipant().getId();
                if (pairParticipantId == participant.getId()) {
                    throw new ScheduleGenerationException("Proposal " + proposal.getId() + " contains the same participant twice");
                }
                CandidatePair pair = new CandidatePair(participant.getId(), pairParticipantId);
                for (int row : qualifiedRows) {
                    proposalPairRequirements
                            .computeIfAbsent(dayIndex, ignored -> new HashMap<>())
                            .computeIfAbsent(row, ignored -> new HashSet<>())
                            .add(pair);
                }
            }
        }

        for (EnforcedParticipant enforcedParticipant : enforcedByDayName.values()) {
            participantsById.put(enforcedParticipant.getParticipant1().getId(), enforcedParticipant.getParticipant1());
            participantsById.put(enforcedParticipant.getParticipant2().getId(), enforcedParticipant.getParticipant2());
        }

        Map<DayCandidateKey, IntRange> effectiveServiceRanges = effectiveRanges(
                qualifiedRowsBySubSheet,
                proposalServiceRanges,
                configuration.configuredServiceRanges()
        );
        Map<DayCandidateKey, BreakRule> effectiveBreakRules = effectiveBreakRules(
                qualifiedRowsBySubSheet,
                proposalBreakRules,
                configuration.configuredBreakRanges()
        );

        Sheet sheet = sheetFor(days, enforcedByDayName);
        List<Candidate> candidates = candidatesFor(qualifiedRowsBySubSheet);
        return new ScheduleGenerationInput(
                sheet,
                candidates,
                participantsById,
                qualifiedRowsBySubSheet,
                limitsBySubSheet(effectiveServiceRanges, false),
                limitsBySubSheet(effectiveServiceRanges, true),
                breakRulesBySubSheet(effectiveBreakRules),
                rowPairRules(proposalPairRequirements),
                configuration.globalPairRules(),
                candidateUniquenessPoints(qualifiedRowsBySubSheet, configuration.candidateUniquenessScoreRules()),
                configuration.participantPrivilegePoints(),
                configuration.pairScorePoints(),
                configuration.pairDiversityPoints()
        );
    }

    private Set<Integer> qualifiedRows(ScheduleGenerationDay day, Proposal proposal) {
        Set<Integer> result = new HashSet<>();
        for (int row = 0; row < day.entries().size(); row++) {
            short hour = day.entries().get(row).getHour();
            if (hour >= proposal.getHourStart() && hour <= proposal.getHourEnd()) {
                result.add(row);
            }
        }
        return result;
    }

    private boolean hasCompleteRange(Short start, Short end, String rangeName, Proposal proposal) {
        if (start == null && end == null) {
            return false;
        }
        if (start == null || end == null) {
            throw new ScheduleGenerationException("Proposal " + proposal.getId() + " has an incomplete " + rangeName);
        }
        return true;
    }

    private IntRange range(Short start, Short end, String description) {
        try {
            return new IntRange(start, end);
        } catch (IllegalArgumentException exception) {
            throw new ScheduleGenerationException("Invalid " + description, exception);
        }
    }

    private void mergeRange(Map<DayCandidateKey, IntRange> ranges, DayCandidateKey key, IntRange additionalRange) {
        IntRange currentRange = ranges.get(key);
        if (currentRange == null) {
            ranges.put(key, additionalRange);
            return;
        }
        int minimum = Math.max(currentRange.minimum(), additionalRange.minimum());
        int maximum = Math.min(currentRange.maximum(), additionalRange.maximum());
        if (minimum > maximum) {
            throw new ScheduleGenerationException(
                    "Conflicting length ranges configured for participant " + key.candidateId()
                            + " on schedule day " + key.dayIndex()
            );
        }
        ranges.put(key, new IntRange(minimum, maximum));
    }

    private BreakRule breakRuleForProposal(Proposal proposal) {
        boolean hasBreakRange = hasCompleteRange(
                proposal.getBreakLengthStart(),
                proposal.getBreakLengthEnd(),
                "break length",
                proposal
        );
        BreakMode breakMode = proposal.getBreakMode();
        if (breakMode == null) {
            // Rows created before the migration are interpreted using the old
            // convention should one ever be read before Flyway has repaired it.
            breakMode = hasBreakRange ? BreakMode.REQUIRE_BREAK : BreakMode.NO_BREAK;
        }
        IntRange breakRange = hasBreakRange
                ? range(proposal.getBreakLengthStart(), proposal.getBreakLengthEnd(), "proposal break length")
                : null;

        return switch (breakMode) {
            case NO_BREAK -> {
                if (breakRange != null) {
                    throw new ScheduleGenerationException(
                            "Proposal " + proposal.getId() + " forbids breaks but contains a break length"
                    );
                }
                yield new BreakRule(BreakRequirement.NO_BREAK, null);
            }
            case REQUIRE_BREAK -> {
                if (breakRange == null) {
                    throw new ScheduleGenerationException(
                            "Proposal " + proposal.getId() + " requires a break but has no break length"
                    );
                }
                yield new BreakRule(BreakRequirement.REQUIRE_BREAK, breakRange);
            }
            case INDIFFERENT -> new BreakRule(BreakRequirement.INDIFFERENT, breakRange);
        };
    }

    private void mergeBreakRule(
            Map<DayCandidateKey, BreakRule> rules,
            DayCandidateKey key,
            BreakRule additionalRule
    ) {
        BreakRule currentRule = rules.get(key);
        if (currentRule == null) {
            rules.put(key, additionalRule);
            return;
        }

        BreakRequirement requirement = mergeBreakRequirements(currentRule.requirement(), additionalRule.requirement(), key);
        IntRange breakRange = requirement == BreakRequirement.NO_BREAK
                ? null
                : intersectBreakRanges(currentRule.allowedBreakLength(), additionalRule.allowedBreakLength(), key);
        try {
            rules.put(key, new BreakRule(requirement, breakRange));
        } catch (IllegalArgumentException exception) {
            throw new ScheduleGenerationException(
                    "Invalid break requirements for participant " + key.candidateId()
                            + " on schedule day " + key.dayIndex(),
                    exception
            );
        }
    }

    private BreakRequirement mergeBreakRequirements(
            BreakRequirement first,
            BreakRequirement second,
            DayCandidateKey key
    ) {
        if (first == second) {
            return first;
        }
        if (first == BreakRequirement.INDIFFERENT) {
            return second;
        }
        if (second == BreakRequirement.INDIFFERENT) {
            return first;
        }
        throw new ScheduleGenerationException(
                "Conflicting break modes configured for participant " + key.candidateId()
                        + " on schedule day " + key.dayIndex()
        );
    }

    private IntRange intersectBreakRanges(IntRange first, IntRange second, DayCandidateKey key) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        int minimum = Math.max(first.minimum(), second.minimum());
        int maximum = Math.min(first.maximum(), second.maximum());
        if (minimum > maximum) {
            throw new ScheduleGenerationException(
                    "Conflicting break ranges configured for participant " + key.candidateId()
                            + " on schedule day " + key.dayIndex()
            );
        }
        return new IntRange(minimum, maximum);
    }

    private Map<DayCandidateKey, IntRange> effectiveRanges(
            Map<Integer, Map<Long, Set<Integer>>> qualifiedRowsBySubSheet,
            Map<DayCandidateKey, IntRange> proposalRanges,
            Map<Long, IntRange> configuredRanges
    ) {
        Map<DayCandidateKey, IntRange> result = new HashMap<>();
        qualifiedRowsBySubSheet.forEach((dayIndex, participants) -> participants.keySet().forEach(candidateId -> {
            DayCandidateKey key = new DayCandidateKey(dayIndex, candidateId);
            IntRange configuredRange = configuredRanges.get(candidateId);
            IntRange proposalRange = proposalRanges.get(key);
            if (configuredRange != null) {
                result.put(key, configuredRange);
            } else if (proposalRange != null) {
                result.put(key, proposalRange);
            }
        }));
        return result;
    }

    private Map<DayCandidateKey, BreakRule> effectiveBreakRules(
            Map<Integer, Map<Long, Set<Integer>>> qualifiedRowsBySubSheet,
            Map<DayCandidateKey, BreakRule> proposalRules,
            Map<Long, IntRange> configuredBreakRanges
    ) {
        Map<DayCandidateKey, BreakRule> result = new HashMap<>();
        qualifiedRowsBySubSheet.forEach((dayIndex, participants) -> participants.keySet().forEach(candidateId -> {
            DayCandidateKey key = new DayCandidateKey(dayIndex, candidateId);
            BreakRule proposalRule = proposalRules.get(key);
            if (proposalRule == null) {
                return;
            }
            IntRange configuredRange = configuredBreakRanges.get(candidateId);
            if (configuredRange != null && proposalRule.requirement() != BreakRequirement.NO_BREAK) {
                result.put(key, proposalRule.withAllowedBreakLength(configuredRange));
            } else {
                result.put(key, proposalRule);
            }
        }));
        return result;
    }

    private Map<Integer, Map<Long, Integer>> limitsBySubSheet(
            Map<DayCandidateKey, IntRange> ranges,
            boolean maximum
    ) {
        Map<Integer, Map<Long, Integer>> result = new HashMap<>();
        ranges.forEach((key, range) -> result
                .computeIfAbsent(key.dayIndex(), ignored -> new HashMap<>())
                .put(key.candidateId(), maximum ? range.maximum() : range.minimum()));
        return result;
    }

    private Map<Integer, Map<Long, BreakRule>> breakRulesBySubSheet(Map<DayCandidateKey, BreakRule> rules) {
        Map<Integer, Map<Long, BreakRule>> result = new HashMap<>();
        rules.forEach((key, rule) -> result
                .computeIfAbsent(key.dayIndex(), ignored -> new HashMap<>())
                .put(key.candidateId(), rule));
        return result;
    }

    private Sheet sheetFor(
            List<ScheduleGenerationDay> days,
            Map<String, EnforcedParticipant> enforcedByDayName
    ) {
        List<SubSheet> subSheets = new ArrayList<>();
        for (ScheduleGenerationDay day : days) {
            SubSheet subSheet = new SubSheet(2, day.entries().size());
            EnforcedParticipant enforcedPair = enforcedByDayName.get(day.day().getName());
            if (enforcedPair != null) {
                Candidate participant1 = new Candidate(enforcedPair.getParticipant1().getId());
                Candidate participant2 = new Candidate(enforcedPair.getParticipant2().getId());
                for (int row = 0; row < day.entries().size(); row++) {
                    subSheet.getCell(0, row).setFixedCandidate(participant1);
                    subSheet.getCell(1, row).setFixedCandidate(participant2);
                }
            }
            subSheets.add(subSheet);
        }
        return new Sheet(subSheets);
    }

    private List<Candidate> candidatesFor(Map<Integer, Map<Long, Set<Integer>>> qualifiedRowsBySubSheet) {
        return qualifiedRowsBySubSheet.values().stream()
                .flatMap(candidateRows -> candidateRows.keySet().stream())
                .distinct()
                .sorted()
                .map(Candidate::new)
                .toList();
    }

    private Map<Integer, Map<Integer, PairRules>> rowPairRules(
            Map<Integer, Map<Integer, Set<CandidatePair>>> proposalPairRequirements
    ) {
        Map<Integer, Map<Integer, PairRules>> result = new HashMap<>();
        proposalPairRequirements.forEach((dayIndex, pairsByRow) -> pairsByRow.forEach((row, requiredPairs) -> result
                .computeIfAbsent(dayIndex, ignored -> new HashMap<>())
                .put(row, new PairRules(requiredPairs, Set.of()))));
        return result;
    }

    private Map<Long, Integer> candidateUniquenessPoints(
            Map<Integer, Map<Long, Set<Integer>>> qualifiedRowsBySubSheet,
            List<CandidateUniquenessScoreRule> configuration
    ) {
        Map<Long, Integer> availabilityCountByCandidate = new HashMap<>();
        qualifiedRowsBySubSheet.values().forEach(candidateRows -> candidateRows.forEach((candidateId, rows) ->
                availabilityCountByCandidate.merge(candidateId, rows.size(), Integer::sum)
        ));

        Map<Long, Integer> result = new HashMap<>();
        availabilityCountByCandidate.forEach((candidateId, appearances) -> {
            int points = configuration.stream()
                    .filter(rule -> appearances >= rule.appearanceCountFrom())
                    .filter(rule -> appearances <= rule.appearanceCountTo())
                    .mapToInt(CandidateUniquenessScoreRule::points)
                    .sum();
            if (points != 0) {
                result.put(candidateId, points);
            }
        });
        return result;
    }

    private record DayCandidateKey(int dayIndex, long candidateId) {
    }
}
