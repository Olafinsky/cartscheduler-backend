package org.cartscheduler.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cartscheduler.entity.ConfigCandidateUniquenessScore;
import org.cartscheduler.entity.ConfigPairDiversityScore;
import org.cartscheduler.entity.ConfigPairRule;
import org.cartscheduler.entity.ConfigPairRuleType;
import org.cartscheduler.entity.ConfigPairScore;
import org.cartscheduler.entity.ConfigParticipantBreakLength;
import org.cartscheduler.entity.ConfigParticipantPrivilegeScore;
import org.cartscheduler.entity.ConfigParticipantServiceLength;
import org.cartscheduler.entity.ConfigRowFullOrEmptyScore;
import org.cartscheduler.entity.EnforcedParticipant;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.Proposal;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.entity.ScheduleEntry;
import org.cartscheduler.generator.AcceptanceFilter;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.CandidatePair;
import org.cartscheduler.generator.Generator;
import org.cartscheduler.generator.IntRange;
import org.cartscheduler.generator.PairRules;
import org.cartscheduler.generator.PostGenerationFilter;
import org.cartscheduler.generator.ScoreFilter;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import org.cartscheduler.generator.acceptancefilter.AcceptanceFilterAppearanceLimit;
import org.cartscheduler.generator.acceptancefilter.AcceptanceFilterCandidateQualifiesForRows;
import org.cartscheduler.generator.acceptancefilter.AcceptanceFilterGapBetweenBlocks;
import org.cartscheduler.generator.acceptancefilter.AcceptanceFilterPairRules;
import org.cartscheduler.generator.acceptancefilter.AcceptanceFilterUniqueInRow;
import org.cartscheduler.generator.postgenerationfilter.PostGenerationFilterMinAppearanceCount;
import org.cartscheduler.generator.postgenerationfilter.PostGenerationFilterPairRules;
import org.cartscheduler.generator.scorefilter.ScoreFilterCandidatePoints;
import org.cartscheduler.generator.scorefilter.ScoreFilterPairDiversity;
import org.cartscheduler.generator.scorefilter.ScoreFilterPairPoints;
import org.cartscheduler.generator.scorefilter.ScoreFilterRowFullOrEmpty;
import org.cartscheduler.repository.ConfigCandidateUniquenessScoreRepository;
import org.cartscheduler.repository.ConfigPairDiversityScoreRepository;
import org.cartscheduler.repository.ConfigPairRuleRepository;
import org.cartscheduler.repository.ConfigPairScoreRepository;
import org.cartscheduler.repository.ConfigParticipantBreakLengthRepository;
import org.cartscheduler.repository.ConfigParticipantPrivilegeScoreRepository;
import org.cartscheduler.repository.ConfigParticipantServiceLengthRepository;
import org.cartscheduler.repository.ConfigRowFullOrEmptyScoreRepository;
import org.cartscheduler.repository.EnforcedParticipantRepository;
import org.cartscheduler.repository.ProposalRepository;
import org.cartscheduler.repository.ScheduleDayRepository;
import org.cartscheduler.repository.ScheduleEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database-facing generator for application schedules. It converts persistent
 * schedule data to the raw {@link Sheet}/{@link SubSheet} model and leaves all
 * search logic in the database-independent {@link Generator}.
 */
@Service
public class ScheduleGenerationService {
    private final ScheduleDayRepository scheduleDayRepository;
    private final ScheduleEntryRepository scheduleEntryRepository;
    private final ProposalRepository proposalRepository;
    private final EnforcedParticipantRepository enforcedParticipantRepository;
    private final ConfigRowFullOrEmptyScoreRepository rowFullOrEmptyScoreRepository;
    private final ConfigPairScoreRepository pairScoreRepository;
    private final ConfigCandidateUniquenessScoreRepository candidateUniquenessScoreRepository;
    private final ConfigParticipantPrivilegeScoreRepository participantPrivilegeScoreRepository;
    private final ConfigPairDiversityScoreRepository pairDiversityScoreRepository;
    private final ConfigPairRuleRepository pairRuleRepository;
    private final ConfigParticipantBreakLengthRepository participantBreakLengthRepository;
    private final ConfigParticipantServiceLengthRepository participantServiceLengthRepository;

    public ScheduleGenerationService(
            ScheduleDayRepository scheduleDayRepository,
            ScheduleEntryRepository scheduleEntryRepository,
            ProposalRepository proposalRepository,
            EnforcedParticipantRepository enforcedParticipantRepository,
            ConfigRowFullOrEmptyScoreRepository rowFullOrEmptyScoreRepository,
            ConfigPairScoreRepository pairScoreRepository,
            ConfigCandidateUniquenessScoreRepository candidateUniquenessScoreRepository,
            ConfigParticipantPrivilegeScoreRepository participantPrivilegeScoreRepository,
            ConfigPairDiversityScoreRepository pairDiversityScoreRepository,
            ConfigPairRuleRepository pairRuleRepository,
            ConfigParticipantBreakLengthRepository participantBreakLengthRepository,
            ConfigParticipantServiceLengthRepository participantServiceLengthRepository
    ) {
        this.scheduleDayRepository = scheduleDayRepository;
        this.scheduleEntryRepository = scheduleEntryRepository;
        this.proposalRepository = proposalRepository;
        this.enforcedParticipantRepository = enforcedParticipantRepository;
        this.rowFullOrEmptyScoreRepository = rowFullOrEmptyScoreRepository;
        this.pairScoreRepository = pairScoreRepository;
        this.candidateUniquenessScoreRepository = candidateUniquenessScoreRepository;
        this.participantPrivilegeScoreRepository = participantPrivilegeScoreRepository;
        this.pairDiversityScoreRepository = pairDiversityScoreRepository;
        this.pairRuleRepository = pairRuleRepository;
        this.participantBreakLengthRepository = participantBreakLengthRepository;
        this.participantServiceLengthRepository = participantServiceLengthRepository;
    }

    @Transactional
    public ScheduleGenerationResult generate(long scheduleId) {
        ConfigRowFullOrEmptyScore rowScoreConfiguration = requiredRowScoreConfiguration();
        List<DayContext> days = loadDays(scheduleId);
        List<Proposal> proposals = proposalRepository.findForSchedule(scheduleId);
        Map<String, EnforcedParticipant> enforcedByDayName = loadEnforcedParticipants(days);

        GenerationInput input = createGenerationInput(days, proposals, enforcedByDayName);
        Generator generator = Generator.forSheet(input.sheet())
                .withCandidates(input.candidates())
                .withAcceptanceFilters(createAcceptanceFilters(input))
                .withPostGenerationFilters(createPostGenerationFilters(input))
                .withScoreFilters(createScoreFilters(input, rowScoreConfiguration));

        Sheet generatedSheet;
        try {
            generatedSheet = generator.generate();
        } catch (IllegalStateException exception) {
            throw new ScheduleGenerationException(
                    "Schedule " + scheduleId + " cannot satisfy the configured generation constraints",
                    exception
            );
        }

        int assignedParticipantCells = persistGeneratedParticipants(days, generatedSheet, input.participantsById());
        return new ScheduleGenerationResult(scheduleId, assignedParticipantCells);
    }

    private ConfigRowFullOrEmptyScore requiredRowScoreConfiguration() {
        List<ConfigRowFullOrEmptyScore> configurations = rowFullOrEmptyScoreRepository.findAll();
        if (configurations.size() != 1) {
            throw new ScheduleGenerationException(
                    "Exactly one config_row_full_or_empty_scores row is required before schedule generation"
            );
        }
        return configurations.getFirst();
    }

    private List<DayContext> loadDays(long scheduleId) {
        List<ScheduleDay> scheduleDays = scheduleDayRepository.findForSchedule(scheduleId);
        if (scheduleDays.isEmpty()) {
            throw new ScheduleGenerationException("Schedule " + scheduleId + " has no schedule days to generate");
        }

        List<ScheduleDay> orderedDays = scheduleDays.stream()
                .sorted(Comparator.comparingInt(ScheduleDay::getDayOfWeek).thenComparingLong(ScheduleDay::getId))
                .toList();
        List<DayContext> result = new ArrayList<>();
        for (int index = 0; index < orderedDays.size(); index++) {
            ScheduleDay day = orderedDays.get(index);
            List<ScheduleEntry> entries = (day.getScheduleEntries() == null ? List.<ScheduleEntry>of() : day.getScheduleEntries())
                    .stream()
                    .sorted(Comparator.comparingInt(ScheduleEntry::getHour).thenComparingLong(ScheduleEntry::getId))
                    .toList();
            result.add(new DayContext(index, day, entries));
        }
        return result;
    }

    private Map<String, EnforcedParticipant> loadEnforcedParticipants(List<DayContext> days) {
        Set<String> dayNames = days.stream().map(context -> context.day().getName()).collect(java.util.stream.Collectors.toSet());
        Map<String, EnforcedParticipant> result = new HashMap<>();
        for (EnforcedParticipant enforcedParticipant : enforcedParticipantRepository.findByDayNameIn(dayNames)) {
            EnforcedParticipant previous = result.put(enforcedParticipant.getDayName(), enforcedParticipant);
            if (previous != null) {
                throw new ScheduleGenerationException(
                        "More than one enforced participant pair exists for day name " + enforcedParticipant.getDayName()
                );
            }
            if (enforcedParticipant.getParticipant1().getId() == enforcedParticipant.getParticipant2().getId()) {
                throw new ScheduleGenerationException(
                        "Enforced participants for day " + enforcedParticipant.getDayName() + " must be different"
                );
            }
        }
        return result;
    }

    private GenerationInput createGenerationInput(
            List<DayContext> days,
            List<Proposal> proposals,
            Map<String, EnforcedParticipant> enforcedByDayName
    ) {
        Map<Long, Integer> dayIndexesById = new HashMap<>();
        for (DayContext day : days) {
            dayIndexesById.put(day.day().getId(), day.index());
        }

        Map<Long, Participant> participantsById = new HashMap<>();
        Map<Integer, Map<Long, Set<Integer>>> qualifiedRowsBySubSheet = new HashMap<>();
        Map<DayCandidateKey, IntRange> proposalServiceRanges = new HashMap<>();
        Map<DayCandidateKey, IntRange> proposalBreakRanges = new HashMap<>();
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

            DayContext day = days.get(dayIndex);
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
            if (hasCompleteRange(proposal.getBreakLengthStart(), proposal.getBreakLengthEnd(), "break length", proposal)) {
                mergeRange(
                        proposalBreakRanges,
                        key,
                        range(proposal.getBreakLengthStart(), proposal.getBreakLengthEnd(), "proposal break length")
                );
            }

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
                configuredServiceRanges()
        );
        Map<DayCandidateKey, IntRange> effectiveBreakRanges = effectiveRanges(
                qualifiedRowsBySubSheet,
                proposalBreakRanges,
                configuredBreakRanges()
        );

        Sheet sheet = sheetFor(days, enforcedByDayName);
        List<Candidate> candidates = candidatesFor(qualifiedRowsBySubSheet);
        return new GenerationInput(
                sheet,
                candidates,
                participantsById,
                qualifiedRowsBySubSheet,
                limitsBySubSheet(effectiveServiceRanges, false),
                limitsBySubSheet(effectiveServiceRanges, true),
                rangesBySubSheet(effectiveBreakRanges),
                rowPairRules(proposalPairRequirements),
                globalPairRules(),
                candidateUniquenessPoints(qualifiedRowsBySubSheet),
                participantPrivilegePoints(),
                pairScorePoints(),
                optionalPairDiversityPoints()
        );
    }

    private Set<Integer> qualifiedRows(DayContext day, Proposal proposal) {
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
                    "Conflicting length ranges configured for participant " + key.candidateId() + " on schedule day " + key.dayIndex()
            );
        }
        ranges.put(key, new IntRange(minimum, maximum));
    }

    private Map<Long, IntRange> configuredServiceRanges() {
        Map<Long, IntRange> result = new HashMap<>();
        for (ConfigParticipantServiceLength configuration : participantServiceLengthRepository.findAll()) {
            putParticipantRange(
                    result,
                    configuration.getParticipant(),
                    range(configuration.getServiceLengthStart(), configuration.getServiceLengthEnd(), "configured service length")
            );
        }
        return result;
    }

    private Map<Long, IntRange> configuredBreakRanges() {
        Map<Long, IntRange> result = new HashMap<>();
        for (ConfigParticipantBreakLength configuration : participantBreakLengthRepository.findAll()) {
            putParticipantRange(
                    result,
                    configuration.getParticipant(),
                    range(configuration.getBreakLengthStart(), configuration.getBreakLengthEnd(), "configured break length")
            );
        }
        return result;
    }

    private void putParticipantRange(Map<Long, IntRange> ranges, Participant participant, IntRange range) {
        IntRange previous = ranges.put(participant.getId(), range);
        if (previous != null) {
            throw new ScheduleGenerationException("More than one length configuration exists for participant " + participant.getId());
        }
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

    private Map<Integer, Map<Long, IntRange>> rangesBySubSheet(Map<DayCandidateKey, IntRange> ranges) {
        Map<Integer, Map<Long, IntRange>> result = new HashMap<>();
        ranges.forEach((key, range) -> result
                .computeIfAbsent(key.dayIndex(), ignored -> new HashMap<>())
                .put(key.candidateId(), range));
        return result;
    }

    private Sheet sheetFor(List<DayContext> days, Map<String, EnforcedParticipant> enforcedByDayName) {
        List<SubSheet> subSheets = new ArrayList<>();
        for (DayContext day : days) {
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

    private PairRules globalPairRules() {
        Set<CandidatePair> requiredPairs = new HashSet<>();
        Set<CandidatePair> forbiddenPairs = new HashSet<>();
        for (ConfigPairRule configuration : pairRuleRepository.findAll()) {
            CandidatePair pair = pair(configuration.getParticipant1(), configuration.getParticipant2(), "pair rule");
            Set<CandidatePair> target = configuration.getRuleType() == ConfigPairRuleType.REQUIRED
                    ? requiredPairs
                    : forbiddenPairs;
            if (!target.add(pair)) {
                throw new ScheduleGenerationException("Duplicate configured " + configuration.getRuleType() + " pair " + pair);
            }
        }
        Set<CandidatePair> conflicts = new HashSet<>(requiredPairs);
        conflicts.retainAll(forbiddenPairs);
        if (!conflicts.isEmpty()) {
            throw new ScheduleGenerationException("A pair cannot be both required and forbidden: " + conflicts.iterator().next());
        }
        return new PairRules(requiredPairs, forbiddenPairs);
    }

    private Map<CandidatePair, Integer> pairScorePoints() {
        Map<CandidatePair, Integer> result = new HashMap<>();
        for (ConfigPairScore configuration : pairScoreRepository.findAll()) {
            CandidatePair pair = pair(configuration.getParticipant1(), configuration.getParticipant2(), "pair score");
            Integer previous = result.put(pair, configuration.getPoints());
            if (previous != null) {
                throw new ScheduleGenerationException("More than one score is configured for pair " + pair);
            }
        }
        return result;
    }

    private CandidatePair pair(Participant first, Participant second, String configurationName) {
        if (first == null || second == null) {
            throw new ScheduleGenerationException("Incomplete " + configurationName + " configuration");
        }
        try {
            return new CandidatePair(first.getId(), second.getId());
        } catch (IllegalArgumentException exception) {
            throw new ScheduleGenerationException("Invalid " + configurationName, exception);
        }
    }

    private Map<Long, Integer> candidateUniquenessPoints(
            Map<Integer, Map<Long, Set<Integer>>> qualifiedRowsBySubSheet
    ) {
        List<ConfigCandidateUniquenessScore> configurations = candidateUniquenessScoreRepository.findAll();
        for (ConfigCandidateUniquenessScore configuration : configurations) {
            if (configuration.getProposalAppearanceCountFrom() < 0
                    || configuration.getProposalAppearanceCountTo() < configuration.getProposalAppearanceCountFrom()) {
                throw new ScheduleGenerationException("Invalid candidate uniqueness score range");
            }
        }

        Map<Long, Integer> availabilityCountByCandidate = new HashMap<>();
        qualifiedRowsBySubSheet.values().forEach(candidateRows -> candidateRows.forEach((candidateId, rows) ->
                availabilityCountByCandidate.merge(candidateId, rows.size(), Integer::sum)
        ));

        Map<Long, Integer> result = new HashMap<>();
        availabilityCountByCandidate.forEach((candidateId, appearances) -> {
            int points = configurations.stream()
                    .filter(configuration -> appearances >= configuration.getProposalAppearanceCountFrom())
                    .filter(configuration -> appearances <= configuration.getProposalAppearanceCountTo())
                    .mapToInt(ConfigCandidateUniquenessScore::getPoints)
                    .sum();
            if (points != 0) {
                result.put(candidateId, points);
            }
        });
        return result;
    }

    private Map<Long, Integer> participantPrivilegePoints() {
        Map<Long, Integer> result = new HashMap<>();
        for (ConfigParticipantPrivilegeScore configuration : participantPrivilegeScoreRepository.findAll()) {
            long participantId = configuration.getParticipant().getId();
            Integer previous = result.put(participantId, configuration.getPoints());
            if (previous != null) {
                throw new ScheduleGenerationException(
                        "More than one privilege score is configured for participant " + participantId
                );
            }
        }
        return result;
    }

    private Integer optionalPairDiversityPoints() {
        List<ConfigPairDiversityScore> configurations = pairDiversityScoreRepository.findAll();
        if (configurations.size() > 1) {
            throw new ScheduleGenerationException("At most one config_pair_diversity_scores row is allowed");
        }
        return configurations.isEmpty() ? null : configurations.getFirst().getPoints();
    }

    private List<AcceptanceFilter> createAcceptanceFilters(GenerationInput input) {
        List<AcceptanceFilter> filters = new ArrayList<>();
        filters.add(new AcceptanceFilterCandidateQualifiesForRows(input.qualifiedRowsBySubSheet()));
        filters.add(new AcceptanceFilterUniqueInRow());
        filters.add(new AcceptanceFilterPairRules(input.globalPairRules(), input.rowPairRulesBySubSheet()));
        filters.add(new AcceptanceFilterAppearanceLimit(input.maximumServiceLengthsBySubSheet()));
        filters.add(new AcceptanceFilterGapBetweenBlocks(input.breakRangesBySubSheet()));
        return filters;
    }

    private List<PostGenerationFilter> createPostGenerationFilters(GenerationInput input) {
        return List.of(
                new PostGenerationFilterPairRules(input.globalPairRules(), input.rowPairRulesBySubSheet()),
                new PostGenerationFilterMinAppearanceCount(input.minimumServiceLengthsBySubSheet())
        );
    }

    private List<ScoreFilter> createScoreFilters(
            GenerationInput input,
            ConfigRowFullOrEmptyScore rowScoreConfiguration
    ) {
        List<ScoreFilter> filters = new ArrayList<>();
        filters.add(new ScoreFilterRowFullOrEmpty(
                rowScoreConfiguration.getEmptyRowPoints(),
                rowScoreConfiguration.getFullRowAdjacentToIncompletePoints(),
                rowScoreConfiguration.getFullRowNotAdjacentToIncompletePoints()
        ));
        if (!input.pairScorePoints().isEmpty()) {
            filters.add(new ScoreFilterPairPoints(input.pairScorePoints()));
        }
        if (!input.candidateUniquenessPoints().isEmpty()) {
            filters.add(new ScoreFilterCandidatePoints(input.candidateUniquenessPoints()));
        }
        if (!input.participantPrivilegePoints().isEmpty()) {
            filters.add(new ScoreFilterCandidatePoints(input.participantPrivilegePoints()));
        }
        if (input.pairDiversityPoints() != null) {
            filters.add(new ScoreFilterPairDiversity(input.pairDiversityPoints()));
        }
        return filters;
    }

    private int persistGeneratedParticipants(
            List<DayContext> days,
            Sheet generatedSheet,
            Map<Long, Participant> participantsById
    ) {
        List<ScheduleEntry> entriesToSave = new ArrayList<>();
        int assignedParticipantCells = 0;
        for (DayContext day : days) {
            SubSheet generatedDay = generatedSheet.getSubSheets().get(day.index());
            for (int row = 0; row < day.entries().size(); row++) {
                ScheduleEntry entry = day.entries().get(row);
                Participant participant1 = participantFor(generatedDay.getCell(0, row).getCandidate(), participantsById);
                Participant participant2 = participantFor(generatedDay.getCell(1, row).getCandidate(), participantsById);
                entry.setParticipant1(participant1);
                entry.setParticipant2(participant2);
                if (participant1 != null) {
                    assignedParticipantCells++;
                }
                if (participant2 != null) {
                    assignedParticipantCells++;
                }
                entriesToSave.add(entry);
            }
        }
        scheduleEntryRepository.saveAll(entriesToSave);
        return assignedParticipantCells;
    }

    private Participant participantFor(Candidate candidate, Map<Long, Participant> participantsById) {
        if (candidate == null) {
            return null;
        }
        Participant participant = participantsById.get(candidate.getId());
        if (participant == null) {
            throw new ScheduleGenerationException("Generated candidate " + candidate.getId() + " is not a participant");
        }
        return participant;
    }

    private record DayContext(int index, ScheduleDay day, List<ScheduleEntry> entries) {
    }

    private record DayCandidateKey(int dayIndex, long candidateId) {
    }

    private record GenerationInput(
            Sheet sheet,
            List<Candidate> candidates,
            Map<Long, Participant> participantsById,
            Map<Integer, Map<Long, Set<Integer>>> qualifiedRowsBySubSheet,
            Map<Integer, Map<Long, Integer>> minimumServiceLengthsBySubSheet,
            Map<Integer, Map<Long, Integer>> maximumServiceLengthsBySubSheet,
            Map<Integer, Map<Long, IntRange>> breakRangesBySubSheet,
            Map<Integer, Map<Integer, PairRules>> rowPairRulesBySubSheet,
            PairRules globalPairRules,
            Map<Long, Integer> candidateUniquenessPoints,
            Map<Long, Integer> participantPrivilegePoints,
            Map<CandidatePair, Integer> pairScorePoints,
            Integer pairDiversityPoints
    ) {
    }
}
