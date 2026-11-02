package org.cartscheduler.service.generation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.cartscheduler.entity.EnforcedParticipant;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.Proposal;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.entity.ScheduleEntry;
import org.cartscheduler.generator.AcceptanceFilter;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.GenerationSearchLimitExceededException;
import org.cartscheduler.generator.Generator;
import org.cartscheduler.generator.GeneratorSearchDeadline;
import org.cartscheduler.generator.OccurrenceScoreRule;
import org.cartscheduler.generator.PostGenerationFilter;
import org.cartscheduler.generator.ScoreFilter;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import org.cartscheduler.generator.acceptancefilter.AcceptanceFilterAppearanceLimit;
import org.cartscheduler.generator.acceptancefilter.AcceptanceFilterCandidateQualifiesForRows;
import org.cartscheduler.generator.acceptancefilter.AcceptanceFilterPairRules;
import org.cartscheduler.generator.acceptancefilter.AcceptanceFilterUniqueInRow;
import org.cartscheduler.generator.postgenerationfilter.PostGenerationFilterBreakRules;
import org.cartscheduler.generator.postgenerationfilter.PostGenerationFilterMinimumAppearanceWhenAssigned;
import org.cartscheduler.generator.postgenerationfilter.PostGenerationFilterPairRules;
import org.cartscheduler.generator.postgenerationfilter.PostGenerationFilterRowsAreFullOrEmpty;
import org.cartscheduler.generator.scorefilter.ScoreFilterCandidatePoints;
import org.cartscheduler.generator.scorefilter.ScoreFilterCandidateAssignmentScores;
import org.cartscheduler.generator.scorefilter.ScoreFilterPairAssignmentScores;
import org.cartscheduler.generator.scorefilter.ScoreFilterPairPoints;
import org.cartscheduler.generator.scorefilter.ScoreFilterRowFullOrEmpty;
import org.cartscheduler.repository.EnforcedParticipantRepository;
import org.cartscheduler.repository.ProposalRepository;
import org.cartscheduler.repository.ScheduleDayRepository;
import org.cartscheduler.repository.ScheduleEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application-facing schedule generator. It orchestrates loading a schedule,
 * choosing proposal alternatives, invoking the raw database-independent
 * {@link Generator}, and persisting the chosen participants.
 */
@Service
public class ScheduleGenerationService {
    private static final Duration DEFAULT_SEARCH_TIME_LIMIT = Duration.ofSeconds(20);
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleGenerationService.class);

    private final ScheduleDayRepository scheduleDayRepository;
    private final ScheduleEntryRepository scheduleEntryRepository;
    private final ProposalRepository proposalRepository;
    private final EnforcedParticipantRepository enforcedParticipantRepository;
    private final ScheduleGenerationConfigurationLoader configurationLoader;
    private final ScheduleGenerationInputFactory inputFactory;

    @Value("${schedule-generation.search-time-limit:PT20S}")
    private Duration searchTimeLimit = DEFAULT_SEARCH_TIME_LIMIT;

    public ScheduleGenerationService(
            ScheduleDayRepository scheduleDayRepository,
            ScheduleEntryRepository scheduleEntryRepository,
            ProposalRepository proposalRepository,
            EnforcedParticipantRepository enforcedParticipantRepository,
            ScheduleGenerationConfigurationLoader configurationLoader,
            ScheduleGenerationInputFactory inputFactory
    ) {
        this.scheduleDayRepository = scheduleDayRepository;
        this.scheduleEntryRepository = scheduleEntryRepository;
        this.proposalRepository = proposalRepository;
        this.enforcedParticipantRepository = enforcedParticipantRepository;
        this.configurationLoader = configurationLoader;
        this.inputFactory = inputFactory;
    }

    @Transactional
    public ScheduleGenerationResult generate(long scheduleId) {
        GeneratedSchedule generatedSchedule = generateSchedule(scheduleId);
        int assignedParticipantCells = persistGeneratedParticipants(
                generatedSchedule.days(),
                generatedSchedule.variant().sheet(),
                generatedSchedule.variant().input().participantsById()
        );
        if (generatedSchedule.variant().searchDeadlineReached()) {
            LOGGER.warn(
                    "Schedule {} generation reached a search-time budget; persisting the best valid schedule found so far",
                    scheduleId
            );
        }
        return new ScheduleGenerationResult(scheduleId, assignedParticipantCells);
    }

    /**
     * Builds a schedule using the same constraints as {@link #generate(long)}
     * without updating schedule entries. This is intended for the console
     * dry-run option and diagnostic use.
     */
    @Transactional(readOnly = true)
    public ScheduleGenerationPreview preview(long scheduleId) {
        GeneratedSchedule generatedSchedule = generateSchedule(scheduleId);
        GeneratedVariant variant = generatedSchedule.variant();
        int assignedParticipantCells = assignedParticipantCells(variant.sheet());
        if (variant.searchDeadlineReached()) {
            LOGGER.warn(
                    "Schedule {} preview reached a search-time budget; returning the best valid schedule found so far",
                    scheduleId
            );
        }
        return new ScheduleGenerationPreview(
                scheduleId,
                assignedParticipantCells,
                variant.score(),
                variant.searchDeadlineReached()
        );
    }

    private GeneratedSchedule generateSchedule(long scheduleId) {
        ScheduleGenerationConfiguration configuration = configurationLoader.load();
        List<ScheduleGenerationDay> days = loadDays(scheduleId);
        List<Proposal> proposals = proposalRepository.findForSchedule(scheduleId);
        Map<String, EnforcedParticipant> enforcedByDayName = loadEnforcedParticipants(days);
        GeneratorSearchDeadline searchDeadline = generationSearchDeadline();
        LOGGER.info("Generating schedule {} with a maximum search time of {}", scheduleId, searchTimeLimit);

        GeneratedVariant generatedVariant = generateBestVariant(
                scheduleId,
                days,
                proposals,
                enforcedByDayName,
                configuration,
                searchDeadline
        );
        return new GeneratedSchedule(days, generatedVariant);
    }

    /**
     * Proposals with the same non-empty alternative group for one participant
     * describe mutually exclusive variants. The raw generator deliberately
     * remains unaware of database proposals, so this application-facing layer
     * searches each group combination and maps only one selected proposal from
     * a group into one generator input.
     */
    private GeneratedVariant generateBestVariant(
            long scheduleId,
            List<ScheduleGenerationDay> days,
            List<Proposal> proposals,
            Map<String, EnforcedParticipant> enforcedByDayName,
            ScheduleGenerationConfiguration configuration,
            GeneratorSearchDeadline searchDeadline
    ) {
        List<Proposal> alwaysIncluded = new ArrayList<>();
        Map<AlternativeGroupKey, List<Proposal>> proposalsByAlternativeGroup = new HashMap<>();

        for (Proposal proposal : proposals) {
            String alternativeGroup = normalizedAlternativeGroup(proposal);
            if (alternativeGroup == null) {
                alwaysIncluded.add(proposal);
                continue;
            }
            AlternativeGroupKey key = new AlternativeGroupKey(proposal.getParticipant().getId(), alternativeGroup);
            proposalsByAlternativeGroup.computeIfAbsent(key, ignored -> new ArrayList<>()).add(proposal);
        }

        List<AlternativeProposalGroup> alternativeGroups = proposalsByAlternativeGroup.entrySet().stream()
                .map(entry -> new AlternativeProposalGroup(
                        entry.getKey().participantId(),
                        entry.getKey().group(),
                        entry.getValue().stream().sorted(Comparator.comparingLong(Proposal::getId)).toList()
                ))
                .sorted(Comparator.comparingLong(AlternativeProposalGroup::participantId)
                        .thenComparing(AlternativeProposalGroup::group))
                .toList();

        List<List<Proposal>> alternativeChoices = new ArrayList<>();
        for (AlternativeProposalGroup alternativeGroup : alternativeGroups) {
            if (alternativeGroup.proposals().size() == 1) {
                // Choosing at most one item from a singleton group adds no
                // constraint, so retain normal proposal behavior without
                // creating an unnecessary search branch.
                alwaysIncluded.addAll(alternativeGroup.proposals());
            } else {
                alternativeChoices.add(alternativeGroup.proposals());
            }
        }

        if (alternativeChoices.isEmpty()) {
            try {
                return generateVariant(
                        inputFactory.create(days, alwaysIncluded, enforcedByDayName, configuration),
                        configuration,
                        searchDeadline
                );
            } catch (GenerationSearchLimitExceededException exception) {
                throw generationSearchTimedOut(scheduleId);
            } catch (IllegalStateException exception) {
                throw cannotSatisfySchedule(scheduleId, exception);
            }
        }

        BestVariant bestVariant = new BestVariant();
        Integer rowOnlyScoreUpperBound = rowOnlyScoreUpperBound(days, configuration);
        searchAlternativeVariants(
                0,
                alternativeChoices,
                new ArrayList<>(alwaysIncluded),
                bestVariant,
                days,
                enforcedByDayName,
                configuration,
                rowOnlyScoreUpperBound,
                searchDeadline
        );
        if (bestVariant.value == null) {
            if (bestVariant.searchDeadlineReached) {
                throw generationSearchTimedOut(scheduleId);
            }
            throw cannotSatisfySchedule(scheduleId, bestVariant.lastFailure);
        }
        return bestVariant.searchDeadlineReached
                ? bestVariant.value.withSearchDeadlineReached()
                : bestVariant.value;
    }

    private void searchAlternativeVariants(
            int groupIndex,
            List<List<Proposal>> alternativeChoices,
            List<Proposal> selectedProposals,
            BestVariant bestVariant,
            List<ScheduleGenerationDay> days,
            Map<String, EnforcedParticipant> enforcedByDayName,
            ScheduleGenerationConfiguration configuration,
            Integer rowOnlyScoreUpperBound,
            GeneratorSearchDeadline searchDeadline
    ) {
        if (bestVariant.reachedScoreUpperBound || bestVariant.searchDeadlineReached) {
            return;
        }
        if (searchDeadline.hasExpired()) {
            bestVariant.searchDeadlineReached = true;
            return;
        }
        if (groupIndex == alternativeChoices.size()) {
            try {
                GeneratedVariant generatedVariant = generateVariant(
                        inputFactory.create(days, selectedProposals, enforcedByDayName, configuration),
                        configuration,
                        searchDeadline
                );
                if (bestVariant.value == null || generatedVariant.score() > bestVariant.value.score()) {
                    bestVariant.value = generatedVariant;
                }
                if (generatedVariant.searchDeadlineReached()) {
                    bestVariant.searchDeadlineReached = true;
                }
                if (rowOnlyScoreUpperBound != null && generatedVariant.score() >= rowOnlyScoreUpperBound) {
                    bestVariant.reachedScoreUpperBound = true;
                }
            } catch (GenerationSearchLimitExceededException exception) {
                bestVariant.searchDeadlineReached = true;
            } catch (IllegalStateException exception) {
                bestVariant.lastFailure = exception;
            }
            return;
        }

        // Try submitted options first. They normally make a feasible,
        // high-scoring schedule available early, which is important when the
        // exhaustive optimization is bounded by a search deadline.
        for (Proposal selectedProposal : alternativeChoices.get(groupIndex)) {
            selectedProposals.add(selectedProposal);
            searchAlternativeVariants(
                    groupIndex + 1,
                    alternativeChoices,
                    selectedProposals,
                    bestVariant,
                    days,
                    enforcedByDayName,
                    configuration,
                    rowOnlyScoreUpperBound,
                    searchDeadline
            );
            selectedProposals.removeLast();
        }

        // No option from a group is also valid: participants are not forced
        // to appear in the schedule merely because they submitted alternatives.
        searchAlternativeVariants(
                groupIndex + 1,
                alternativeChoices,
                selectedProposals,
                bestVariant,
                days,
                enforcedByDayName,
                configuration,
                rowOnlyScoreUpperBound,
                searchDeadline
        );
    }

    /**
     * When only the mandatory row score is configured, this is a strict upper
     * bound for every alternative-proposal combination. Reaching it avoids
     * rerunning the raw generator for combinations that cannot improve the
     * result.
     */
    private Integer rowOnlyScoreUpperBound(
            List<ScheduleGenerationDay> days,
            ScheduleGenerationConfiguration configuration
    ) {
        if (!configuration.hasOnlyRowScore()) {
            return null;
        }

        RowFullOrEmptyScore rowScore = configuration.rowScore();
        int maximumRowScore = Math.max(
                0,
                Math.max(
                        rowScore.emptyRowPoints(),
                        Math.max(
                                rowScore.fullRowAdjacentToIncompletePoints(),
                                rowScore.fullRowNotAdjacentToIncompletePoints()
                        )
                )
        );
        int rowCount = days.stream().mapToInt(day -> day.entries().size()).sum();
        return Math.multiplyExact(rowCount, maximumRowScore);
    }

    private GeneratedVariant generateVariant(
            ScheduleGenerationInput input,
            ScheduleGenerationConfiguration configuration,
            GeneratorSearchDeadline searchDeadline
    ) {
        List<AcceptanceFilter> acceptanceFilters = createAcceptanceFilters(input);
        List<PostGenerationFilter> postGenerationFilters = createPostGenerationFilters(input);
        List<ScoreFilter> scoreFilters = createScoreFilters(input, configuration.rowScore());
        Generator generator = Generator.forSheet(input.sheet())
                .withCandidates(input.candidates())
                .withAcceptanceFilters(acceptanceFilters)
                .withPostGenerationFilters(postGenerationFilters)
                .withScoreFilters(scoreFilters)
                .withEquivalentRowPositions()
                .withStaticCandidateDomains()
                .withSearchDeadline(searchDeadline);

        Sheet generatedSheet = generator.generate();
        int score = generatedSheet.getSubSheets().stream()
                .mapToInt(subSheet -> scoreFilters.stream()
                        .mapToInt(filter -> filter.calculateScore(generatedSheet, subSheet))
                        .sum())
                .sum();
        return new GeneratedVariant(input, generatedSheet, score, generator.isSearchDeadlineReached());
    }

    private GeneratorSearchDeadline generationSearchDeadline() {
        if (searchTimeLimit == null || searchTimeLimit.isNegative() || searchTimeLimit.isZero()) {
            throw new ScheduleGenerationException("schedule-generation.search-time-limit must be greater than zero");
        }
        return GeneratorSearchDeadline.after(searchTimeLimit);
    }

    private ScheduleGenerationException cannotSatisfySchedule(long scheduleId, IllegalStateException cause) {
        if (cause == null) {
            return new ScheduleGenerationException(
                    "Schedule " + scheduleId + " cannot satisfy the configured generation constraints"
            );
        }
        return new ScheduleGenerationException(
                "Schedule " + scheduleId + " cannot satisfy the configured generation constraints: " + cause.getMessage(),
                cause
        );
    }

    private ScheduleGenerationException generationSearchTimedOut(long scheduleId) {
        return new ScheduleGenerationException(
                "Schedule " + scheduleId + " could not be generated within the configured search time limit of "
                        + searchTimeLimit
        );
    }

    private String normalizedAlternativeGroup(Proposal proposal) {
        String alternativeGroup = proposal.getAlternativeGroup();
        if (alternativeGroup == null) {
            return null;
        }
        String normalizedGroup = alternativeGroup.strip();
        return normalizedGroup.isEmpty() ? null : normalizedGroup;
    }

    private List<ScheduleGenerationDay> loadDays(long scheduleId) {
        List<ScheduleDay> scheduleDays = scheduleDayRepository.findForSchedule(scheduleId);
        if (scheduleDays.isEmpty()) {
            throw new ScheduleGenerationException("Schedule " + scheduleId + " has no schedule days to generate");
        }

        List<ScheduleDay> orderedDays = scheduleDays.stream()
                .sorted(Comparator.comparingInt(ScheduleDay::getDayOfWeek).thenComparingLong(ScheduleDay::getId))
                .toList();
        List<ScheduleGenerationDay> result = new ArrayList<>();
        for (int index = 0; index < orderedDays.size(); index++) {
            ScheduleDay day = orderedDays.get(index);
            List<ScheduleEntry> entries = (day.getScheduleEntries() == null
                    ? List.<ScheduleEntry>of()
                    : day.getScheduleEntries())
                    .stream()
                    .sorted(Comparator.comparingInt(ScheduleEntry::getHour).thenComparingLong(ScheduleEntry::getId))
                    .toList();
            result.add(new ScheduleGenerationDay(index, day, entries));
        }
        return result;
    }

    private Map<String, EnforcedParticipant> loadEnforcedParticipants(List<ScheduleGenerationDay> days) {
        Set<String> dayNames = days.stream()
                .map(context -> context.day().getName())
                .collect(Collectors.toSet());
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

    private List<AcceptanceFilter> createAcceptanceFilters(ScheduleGenerationInput input) {
        return List.of(
                new AcceptanceFilterCandidateQualifiesForRows(input.qualifiedRowsBySubSheet()),
                new AcceptanceFilterUniqueInRow(),
                new AcceptanceFilterPairRules(input.globalPairRules(), input.rowPairRulesBySubSheet()),
                new AcceptanceFilterAppearanceLimit(input.maximumServiceLengthsBySubSheet())
        );
    }

    private List<PostGenerationFilter> createPostGenerationFilters(ScheduleGenerationInput input) {
        return List.of(
                new PostGenerationFilterRowsAreFullOrEmpty(),
                new PostGenerationFilterPairRules(input.globalPairRules(), input.rowPairRulesBySubSheet()),
                new PostGenerationFilterMinimumAppearanceWhenAssigned(input.minimumServiceLengthsBySubSheet()),
                new PostGenerationFilterBreakRules(input.breakRulesBySubSheet())
        );
    }

    private List<ScoreFilter> createScoreFilters(
            ScheduleGenerationInput input,
            RowFullOrEmptyScore rowScore
    ) {
        List<ScoreFilter> filters = new ArrayList<>();
        filters.add(new ScoreFilterRowFullOrEmpty(
                rowScore.emptyRowPoints(),
                rowScore.fullRowAdjacentToIncompletePoints(),
                rowScore.fullRowNotAdjacentToIncompletePoints()
        ));
        if (!input.pairScorePoints().isEmpty()) {
            filters.add(new ScoreFilterPairPoints(input.pairScorePoints()));
        }
        if (OccurrenceScoreRule.hasNonZeroPoints(input.candidateAssignmentScoreRules())) {
            filters.add(new ScoreFilterCandidateAssignmentScores(input.candidateAssignmentScoreRules()));
        }
        if (!input.participantPrivilegePoints().isEmpty()) {
            filters.add(new ScoreFilterCandidatePoints(input.participantPrivilegePoints()));
        }
        if (OccurrenceScoreRule.hasNonZeroPoints(input.pairAssignmentScoreRules())) {
            filters.add(new ScoreFilterPairAssignmentScores(input.pairAssignmentScoreRules()));
        }
        return filters;
    }

    private int persistGeneratedParticipants(
            List<ScheduleGenerationDay> days,
            Sheet generatedSheet,
            Map<Long, Participant> participantsById
    ) {
        List<ScheduleEntry> entriesToSave = new ArrayList<>();
        int assignedParticipantCells = 0;
        for (ScheduleGenerationDay day : days) {
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

    private int assignedParticipantCells(Sheet generatedSheet) {
        return (int) generatedSheet.getSubSheets().stream()
                .flatMap(subSheet -> subSheet.getCells().stream())
                .filter(cell -> cell.getCandidate() != null)
                .count();
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

    private record AlternativeGroupKey(long participantId, String group) {
    }

    private record AlternativeProposalGroup(long participantId, String group, List<Proposal> proposals) {
    }

    private record GeneratedVariant(
            ScheduleGenerationInput input,
            Sheet sheet,
            int score,
            boolean searchDeadlineReached
    ) {
        private GeneratedVariant withSearchDeadlineReached() {
            return new GeneratedVariant(input, sheet, score, true);
        }
    }

    private record GeneratedSchedule(List<ScheduleGenerationDay> days, GeneratedVariant variant) {
    }

    private static final class BestVariant {
        private GeneratedVariant value;
        private IllegalStateException lastFailure;
        private boolean reachedScoreUpperBound;
        private boolean searchDeadlineReached;
    }
}
