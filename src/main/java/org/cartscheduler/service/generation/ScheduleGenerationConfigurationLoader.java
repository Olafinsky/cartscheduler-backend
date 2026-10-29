package org.cartscheduler.service.generation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cartscheduler.entity.ConfigCandidateAssignmentScore;
import org.cartscheduler.entity.ConfigGeneration;
import org.cartscheduler.entity.ConfigPairAssignmentScore;
import org.cartscheduler.entity.ConfigPairCompatibilityMode;
import org.cartscheduler.entity.ConfigPairRule;
import org.cartscheduler.entity.ConfigParticipantRule;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.generator.CandidatePair;
import org.cartscheduler.generator.IntRange;
import org.cartscheduler.generator.OccurrenceScoreRule;
import org.cartscheduler.generator.PairRules;
import org.cartscheduler.repository.ConfigCandidateAssignmentScoreRepository;
import org.cartscheduler.repository.ConfigGenerationRepository;
import org.cartscheduler.repository.ConfigPairAssignmentScoreRepository;
import org.cartscheduler.repository.ConfigPairRuleRepository;
import org.cartscheduler.repository.ConfigParticipantRuleRepository;
import org.springframework.stereotype.Component;

/**
 * Loads and validates the persistent, globally applicable generation rules.
 * It intentionally returns only database-independent values so that mapping
 * proposal variants never needs to query configuration repositories again.
 */
@Component
class ScheduleGenerationConfigurationLoader {
    private final ConfigGenerationRepository generationRepository;
    private final ConfigParticipantRuleRepository participantRuleRepository;
    private final ConfigPairRuleRepository pairRuleRepository;
    private final ConfigCandidateAssignmentScoreRepository candidateAssignmentScoreRepository;
    private final ConfigPairAssignmentScoreRepository pairAssignmentScoreRepository;

    ScheduleGenerationConfigurationLoader(
            ConfigGenerationRepository generationRepository,
            ConfigParticipantRuleRepository participantRuleRepository,
            ConfigPairRuleRepository pairRuleRepository,
            ConfigCandidateAssignmentScoreRepository candidateAssignmentScoreRepository,
            ConfigPairAssignmentScoreRepository pairAssignmentScoreRepository
    ) {
        this.generationRepository = generationRepository;
        this.participantRuleRepository = participantRuleRepository;
        this.pairRuleRepository = pairRuleRepository;
        this.candidateAssignmentScoreRepository = candidateAssignmentScoreRepository;
        this.pairAssignmentScoreRepository = pairAssignmentScoreRepository;
    }

    ScheduleGenerationConfiguration load() {
        ConfigGeneration generation = requiredGenerationConfiguration();
        ParticipantConfiguration participantConfiguration = participantConfiguration();
        PairConfiguration pairConfiguration = pairConfiguration();

        return new ScheduleGenerationConfiguration(
                new RowFullOrEmptyScore(
                        generation.getEmptyRowPoints(),
                        generation.getFullRowAdjacentToIncompletePoints(),
                        generation.getFullRowNotAdjacentToIncompletePoints()
                ),
                participantConfiguration.serviceRanges(),
                participantConfiguration.breakRanges(),
                pairConfiguration.rules(),
                pairConfiguration.scorePoints(),
                candidateAssignmentScoreRules(),
                participantConfiguration.privilegePoints(),
                pairAssignmentScoreRules()
        );
    }

    private ConfigGeneration requiredGenerationConfiguration() {
        return generationRepository.findById(ConfigGeneration.SINGLETON_ID)
                .orElseThrow(() -> new ScheduleGenerationException(
                        "The config_generation row with id 1 is required before schedule generation"
                ));
    }

    private ParticipantConfiguration participantConfiguration() {
        Map<Long, IntRange> serviceRanges = new HashMap<>();
        Map<Long, IntRange> breakRanges = new HashMap<>();
        Map<Long, Integer> privilegePoints = new HashMap<>();

        for (ConfigParticipantRule configuration : participantRuleRepository.findAll()) {
            long participantId = participantId(configuration.getParticipant(), "participant rule configuration");
            IntRange serviceRange = optionalRange(
                    configuration.getServiceLengthMin(),
                    configuration.getServiceLengthMax(),
                    "configured service length"
            );
            IntRange breakRange = optionalRange(
                    configuration.getBreakLengthMin(),
                    configuration.getBreakLengthMax(),
                    "configured break length"
            );

            putParticipantRange(serviceRanges, participantId, serviceRange, "configured service length");
            putParticipantRange(breakRanges, participantId, breakRange, "configured break length");
            if (configuration.getPrivilegePoints() != null) {
                Integer previous = privilegePoints.put(participantId, configuration.getPrivilegePoints());
                if (previous != null) {
                    throw new ScheduleGenerationException(
                            "More than one privilege score is configured for participant " + participantId
                    );
                }
            }
        }
        return new ParticipantConfiguration(serviceRanges, breakRanges, privilegePoints);
    }

    private void putParticipantRange(
            Map<Long, IntRange> ranges,
            long participantId,
            IntRange range,
            String configurationName
    ) {
        if (range == null) {
            return;
        }
        IntRange previous = ranges.put(participantId, range);
        if (previous != null) {
            throw new ScheduleGenerationException(
                    "More than one " + configurationName + " is configured for participant " + participantId
            );
        }
    }

    private IntRange optionalRange(Short start, Short end, String description) {
        if (start == null && end == null) {
            return null;
        }
        if (start == null || end == null) {
            throw new ScheduleGenerationException("Incomplete " + description);
        }
        try {
            return new IntRange(start, end);
        } catch (IllegalArgumentException exception) {
            throw new ScheduleGenerationException("Invalid " + description, exception);
        }
    }

    private PairConfiguration pairConfiguration() {
        Set<CandidatePair> requiredPairs = new HashSet<>();
        Set<CandidatePair> forbiddenPairs = new HashSet<>();
        Map<CandidatePair, Integer> scorePoints = new HashMap<>();
        Set<CandidatePair> configuredPairs = new HashSet<>();

        for (ConfigPairRule configuration : pairRuleRepository.findAll()) {
            CandidatePair pair = pair(configuration.getParticipant1(), configuration.getParticipant2(), "pair rule");
            if (!configuredPairs.add(pair)) {
                throw new ScheduleGenerationException("More than one configuration exists for pair " + pair);
            }

            ConfigPairCompatibilityMode compatibilityMode = configuration.getCompatibilityMode();
            if (compatibilityMode == null) {
                throw new ScheduleGenerationException("Incomplete pair rule configuration");
            }
            switch (compatibilityMode) {
                case ANY -> {
                    // No acceptance constraint is necessary for an unrestricted pair.
                }
                case REQUIRED -> requiredPairs.add(pair);
                case FORBIDDEN -> forbiddenPairs.add(pair);
            }

            if (configuration.getScorePoints() != 0) {
                scorePoints.put(pair, configuration.getScorePoints());
            }
        }
        return new PairConfiguration(new PairRules(requiredPairs, forbiddenPairs), scorePoints);
    }

    private List<OccurrenceScoreRule> candidateAssignmentScoreRules() {
        List<OccurrenceScoreRule> result = new ArrayList<>();
        for (ConfigCandidateAssignmentScore configuration : candidateAssignmentScoreRepository.findAll()) {
            result.add(occurrenceScoreRule(
                    configuration.getAssignmentCountFrom(),
                    configuration.getAssignmentCountTo(),
                    configuration.getPoints(),
                    "candidate assignment score"
            ));
        }
        return validatedOccurrenceScoreRules(result, "candidate assignment score");
    }

    private List<OccurrenceScoreRule> pairAssignmentScoreRules() {
        List<OccurrenceScoreRule> result = new ArrayList<>();
        for (ConfigPairAssignmentScore configuration : pairAssignmentScoreRepository.findAll()) {
            result.add(occurrenceScoreRule(
                    configuration.getAssignmentCountFrom(),
                    configuration.getAssignmentCountTo(),
                    configuration.getPoints(),
                    "pair assignment score"
            ));
        }
        return validatedOccurrenceScoreRules(result, "pair assignment score");
    }

    private OccurrenceScoreRule occurrenceScoreRule(int from, int to, int points, String configurationName) {
        try {
            return new OccurrenceScoreRule(from, to, points);
        } catch (IllegalArgumentException exception) {
            throw new ScheduleGenerationException("Invalid " + configurationName + " range", exception);
        }
    }

    private List<OccurrenceScoreRule> validatedOccurrenceScoreRules(
            List<OccurrenceScoreRule> rules,
            String configurationName
    ) {
        try {
            return OccurrenceScoreRule.sortedAndValidated(rules);
        } catch (IllegalArgumentException exception) {
            throw new ScheduleGenerationException("Overlapping " + configurationName + " ranges", exception);
        }
    }

    private CandidatePair pair(Participant first, Participant second, String configurationName) {
        try {
            return new CandidatePair(
                    participantId(first, configurationName),
                    participantId(second, configurationName)
            );
        } catch (IllegalArgumentException exception) {
            throw new ScheduleGenerationException("Invalid " + configurationName, exception);
        }
    }

    private long participantId(Participant participant, String configurationName) {
        if (participant == null) {
            throw new ScheduleGenerationException("Incomplete " + configurationName);
        }
        return participant.getId();
    }

    private record ParticipantConfiguration(
            Map<Long, IntRange> serviceRanges,
            Map<Long, IntRange> breakRanges,
            Map<Long, Integer> privilegePoints
    ) {
    }

    private record PairConfiguration(PairRules rules, Map<CandidatePair, Integer> scorePoints) {
    }
}
