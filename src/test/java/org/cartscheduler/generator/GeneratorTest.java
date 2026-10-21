package org.cartscheduler.generator;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.LongStream;
import org.cartscheduler.generator.acceptancefilter.AcceptanceFilterAppearanceLimit;
import org.cartscheduler.generator.acceptancefilter.AcceptanceFilterCandidateQualifiesForRows;
import org.cartscheduler.generator.acceptancefilter.AcceptanceFilterPairRules;
import org.cartscheduler.generator.acceptancefilter.AcceptanceFilterUniqueInRow;
import org.cartscheduler.generator.postgenerationfilter.PostGenerationFilterBreakRules;
import org.cartscheduler.generator.postgenerationfilter.PostGenerationFilterMinAppearanceCount;
import org.cartscheduler.generator.postgenerationfilter.PostGenerationFilterPairRules;
import org.cartscheduler.generator.scorefilter.ScoreFilterRowFullOrEmpty;
import org.cartscheduler.generator.scorefilter.ScoreFilterCandidatePoints;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneratorTest {

    @Test
    void evaluatesCompletePostGenerationFiltersOnlyForCompleteAssignments() {
        Sheet sheet = new Sheet(List.of(new SubSheet(1, 2)));
        AtomicInteger postGenerationFilterCalls = new AtomicInteger();
        PostGenerationFilter counter = (generatedSheet, subSheet) -> {
            postGenerationFilterCalls.incrementAndGet();
            return true;
        };

        Generator.forSheet(sheet)
                .withCandidates(List.of(new Candidate(1L)))
                .withPostGenerationFilters(List.of(counter))
                // A generic score filter deliberately has no upper-bound
                // contract, therefore all complete assignments are visited.
                .withScoreFilters(List.of((generatedSheet, subSheet) -> 0))
                .generate();

        // Two cells, each with one candidate or null: 2 * 2 complete states,
        // plus the initial all-empty valid baseline.
        assertThat(postGenerationFilterCalls.get()).isEqualTo(5);
    }

    @Test
    void prunesEquivalentBranchesAfterFindingTheTheoreticalBestScore() {
        Sheet sheet = new Sheet(List.of(new SubSheet(2, 11)));
        List<Candidate> candidates = LongStream.rangeClosed(1, 16)
                .mapToObj(Candidate::new)
                .toList();
        AtomicInteger postGenerationFilterCalls = new AtomicInteger();
        PostGenerationFilter counter = (generatedSheet, subSheet) -> {
            postGenerationFilterCalls.incrementAndGet();
            return true;
        };

        Sheet generatedSheet = Generator.forSheet(sheet)
                .withCandidates(candidates)
                .withAcceptanceFilters(List.of(new AcceptanceFilterUniqueInRow()))
                .withPostGenerationFilters(List.of(counter))
                .withScoreFilters(List.of(new ScoreFilterRowFullOrEmpty(1, 2, 3)))
                .generate();

        assertThat(postGenerationFilterCalls.get()).isEqualTo(2);
        assertThat(generatedSheet.getSubSheets().getFirst().getCells())
                .allSatisfy(cell -> assertThat(cell.getCandidate()).isNotNull());
    }

    @Test
    void prunesRequiredPairBranchesWhosePartnerCannotBeAssigned() {
        Sheet sheet = new Sheet(List.of(new SubSheet(2, 1)));
        AtomicInteger completeAssignments = new AtomicInteger();
        PostGenerationFilter counter = (generatedSheet, subSheet) -> {
            completeAssignments.incrementAndGet();
            return true;
        };
        PairRules pairRules = new PairRules(Set.of(new CandidatePair(1L, 2L)), Set.of());

        Sheet generatedSheet = Generator.forSheet(sheet)
                .withCandidates(List.of(new Candidate(1L)))
                .withPostGenerationFilters(List.of(
                        new PostGenerationFilterPairRules(pairRules),
                        counter
                ))
                .withScoreFilters(List.of((generatedSheetValue, subSheet) -> 0))
                .generate();

        assertThat(completeAssignments.get()).isEqualTo(2);
        assertThat(generatedSheet.getSubSheets().getFirst().getCells())
                .allSatisfy(cell -> assertThat(cell.getCandidate()).isNull());
    }

    @Test
    void rejectsAnUnreachableMinimumAppearanceBeforeEnumeratingAssignments() {
        Sheet sheet = new Sheet(List.of(new SubSheet(1, 1)));

        assertThatThrownBy(() -> Generator.forSheet(sheet)
                .withCandidates(List.of())
                .withPostGenerationFilters(List.of(new PostGenerationFilterMinAppearanceCount(1L, 1)))
                .generate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No assignment satisfies");
    }

    @Test
    void triesCandidatesWithUnmetMinimumAppearancesFirst() {
        Sheet sheet = new Sheet(List.of(new SubSheet(1, 2)));
        AtomicReference<Long> firstValidAssignment = new AtomicReference<>();
        PostGenerationFilter captureFirstValidAssignment = (generatedSheet, subSheet) -> {
            if (firstValidAssignment.get() == null) {
                Candidate firstCandidate = subSheet.getCell(0, 0).getCandidate();
                firstValidAssignment.set(firstCandidate == null ? -1L : firstCandidate.getId());
            }
            return true;
        };

        Generator.forSheet(sheet)
                .withCandidates(List.of(new Candidate(1L), new Candidate(2L)))
                .withPostGenerationFilters(List.of(
                        new PostGenerationFilterMinAppearanceCount(2L, 1),
                        captureFirstValidAssignment
                ))
                .withScoreFilters(List.of((generatedSheet, subSheet) -> 0))
                .generate();

        assertThat(firstValidAssignment.get()).isEqualTo(2L);
    }

    @Test
    void keepsSearchingWhenTheOptimisticBoundShowsThatTheScoreCanImprove() {
        Sheet sheet = new Sheet(List.of(new SubSheet(1, 1)));

        Sheet generatedSheet = Generator.forSheet(sheet)
                .withCandidates(List.of(new Candidate(1L), new Candidate(2L)))
                .withScoreFilters(List.of(new ScoreFilterCandidatePoints(Map.of(1L, 1, 2L, 5))))
                .generate();

        assertThat(generatedSheet.getSubSheets().getFirst().getCell(0, 0).getCandidate().getId())
                .isEqualTo(2L);
    }

    @Test
    void avoidsMirroredAssignmentsWhenRowPositionsAreEquivalent() {
        Sheet sheet = new Sheet(List.of(new SubSheet(2, 1)));
        AtomicInteger completeAssignments = new AtomicInteger();
        PostGenerationFilter counter = (generatedSheet, subSheet) -> {
            completeAssignments.incrementAndGet();
            return true;
        };

        Sheet generatedSheet = Generator.forSheet(sheet)
                .withCandidates(List.of(new Candidate(1L), new Candidate(2L)))
                .withAcceptanceFilters(List.of(new AcceptanceFilterUniqueInRow()))
                .withPostGenerationFilters(List.of(counter))
                .withScoreFilters(List.of((generatedSheetValue, subSheet) -> 0))
                .withEquivalentRowPositions()
                .generate();

        // Canonical rows are: [1,2], [1,null], [2,null] and [null,null],
        // plus the initial all-empty valid baseline.
        assertThat(completeAssignments.get()).isEqualTo(5);
        assertThat(generatedSheet.getSubSheets().getFirst().getCell(0, 0).getCandidate().getId()).isEqualTo(1L);
        assertThat(generatedSheet.getSubSheets().getFirst().getCell(1, 0).getCandidate().getId()).isEqualTo(2L);
    }

    @Test
    void failsQuicklyWhenTheSharedSearchDeadlineIsAlreadyExpired() {
        Sheet sheet = new Sheet(List.of(new SubSheet(1, 1)));

        assertThatThrownBy(() -> Generator.forSheet(sheet)
                .withCandidates(List.of(new Candidate(1L)))
                .withPostGenerationFilters(List.of(new PostGenerationFilterMinAppearanceCount(1L, 1)))
                .withSearchDeadline(GeneratorSearchDeadline.after(Duration.ZERO))
                .generate())
                .isInstanceOf(GenerationSearchLimitExceededException.class)
                .hasMessageContaining("deadline");
    }

    @Test
    void returnsAnEmptyOptionalScheduleWhenTheSharedSearchDeadlineIsAlreadyExpired() {
        Sheet generatedSheet = Generator.forSheet(new Sheet(List.of(new SubSheet(1, 1))))
                .withCandidates(List.of(new Candidate(1L)))
                .withSearchDeadline(GeneratorSearchDeadline.after(Duration.ZERO))
                .generate();

        assertThat(generatedSheet.getSubSheets().getFirst().getCell(0, 0).getCandidate()).isNull();
    }

    @Test
    void returnsTheBestValidAssignmentFoundBeforeTheDeadline() {
        Sheet sheet = new Sheet(List.of(new SubSheet(1, 9)));
        AtomicInteger postGenerationFilterCalls = new AtomicInteger();
        PostGenerationFilter delayedSecondResult = (generatedSheet, subSheet) -> {
            if (postGenerationFilterCalls.incrementAndGet() == 2) {
                try {
                    Thread.sleep(75);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
            }
            return true;
        };

        Generator generator = Generator.forSheet(sheet)
                .withCandidates(List.of(new Candidate(1L)))
                .withPostGenerationFilters(List.of(delayedSecondResult))
                .withScoreFilters(List.of((generatedSheet, subSheet) -> 0))
                .withSearchDeadline(GeneratorSearchDeadline.after(Duration.ofMillis(50)));

        Sheet generatedSheet = generator.generate();

        assertThat(generator.isSearchDeadlineReached()).isTrue();
        assertThat(postGenerationFilterCalls.get()).isGreaterThan(0);
        assertThat(generatedSheet.getSubSheets().getFirst().getCell(0, 0).getCandidate()).isNotNull();
    }

    @Test
    void givesEachSubSheetPartOfTheSharedSearchTime() {
        Sheet sheet = new Sheet(List.of(new SubSheet(1, 8), new SubSheet(1, 1)));
        AtomicInteger firstDayFilterCalls = new AtomicInteger();
        PostGenerationFilter delaysTheFirstDay = (generatedSheet, subSheet) -> {
            if (generatedSheet.getSubSheets().indexOf(subSheet) == 0
                    && firstDayFilterCalls.incrementAndGet() == 2) {
                try {
                    Thread.sleep(600);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
            }
            return true;
        };
        PostGenerationFilter onlyTheSecondCandidateFitsTheSecondDay = (generatedSheet, subSheet) -> {
            if (generatedSheet.getSubSheets().indexOf(subSheet) != 1) {
                return true;
            }
            Candidate candidate = subSheet.getCell(0, 0).getCandidate();
            return candidate == null || candidate.getId() == 2L;
        };

        Sheet generatedSheet = Generator.forSheet(sheet)
                .withCandidates(List.of(new Candidate(1L), new Candidate(2L)))
                .withPostGenerationFilters(List.of(delaysTheFirstDay, onlyTheSecondCandidateFitsTheSecondDay))
                .withScoreFilters(List.of((generatedSheetValue, subSheet) -> 0))
                .withSearchDeadline(GeneratorSearchDeadline.after(Duration.ofSeconds(1)))
                .generate();

        assertThat(generatedSheet.getSubSheets().get(1).getCell(0, 0).getCandidate().getId()).isEqualTo(2L);
    }

    @Test
    void findsAConstrainedElevenRowScheduleBeforeTheSearchDeadline() {
        Map<Long, Set<Integer>> qualifiedRows = Map.ofEntries(
                Map.entry(6L, Set.of(10)),
                Map.entry(10L, rows(0, 10)),
                Map.entry(11L, rows(3, 6)),
                Map.entry(12L, rows(2, 10)),
                Map.entry(13L, Set.of(10)),
                Map.entry(14L, rows(0, 10)),
                Map.entry(15L, rows(3, 6)),
                Map.entry(16L, rows(3, 6)),
                Map.entry(17L, rows(3, 6)),
                Map.entry(19L, rows(8, 10)),
                Map.entry(20L, rows(0, 10)),
                Map.entry(21L, rows(1, 6)),
                Map.entry(22L, rows(3, 10)),
                Map.entry(24L, rows(2, 10))
        );
        Map<Long, Integer> minimums = Map.ofEntries(
                Map.entry(6L, 1), Map.entry(10L, 1), Map.entry(11L, 2), Map.entry(12L, 1),
                Map.entry(13L, 1), Map.entry(14L, 1), Map.entry(15L, 1), Map.entry(16L, 1),
                Map.entry(17L, 2), Map.entry(19L, 1), Map.entry(20L, 1),
                Map.entry(21L, 1), Map.entry(22L, 1), Map.entry(24L, 1)
        );
        Map<Long, Integer> maximums = Map.ofEntries(
                Map.entry(6L, 1), Map.entry(10L, 2), Map.entry(11L, 2), Map.entry(12L, 2),
                Map.entry(13L, 1), Map.entry(14L, 2), Map.entry(15L, 1), Map.entry(16L, 1),
                Map.entry(17L, 2), Map.entry(19L, 1), Map.entry(20L, 2),
                Map.entry(21L, 2), Map.entry(22L, 2), Map.entry(24L, 2)
        );
        Map<Long, BreakRule> breakRules = Map.of(
                12L, requiredBreak(2, 8),
                21L, requiredBreak(2, 5),
                22L, requiredBreak(2, 7),
                24L, requiredBreak(2, 8)
        );
        PairRules requestedPair = new PairRules(Set.of(new CandidatePair(15L, 16L)), Set.of());
        Map<Integer, PairRules> rowPairRules = Map.of(
                3, requestedPair,
                4, requestedPair,
                5, requestedPair,
                6, requestedPair
        );
        List<Candidate> candidates = qualifiedRows.keySet().stream().sorted().map(Candidate::new).toList();
        Generator generator = Generator.forSheet(new Sheet(List.of(new SubSheet(2, 11))))
                .withCandidates(candidates)
                .withAcceptanceFilters(List.of(
                        new AcceptanceFilterCandidateQualifiesForRows(Map.of(0, qualifiedRows)),
                        new AcceptanceFilterUniqueInRow(),
                        new AcceptanceFilterPairRules(PairRules.empty(), Map.of(0, rowPairRules)),
                        new AcceptanceFilterAppearanceLimit(Map.of(0, maximums))
                ))
                .withPostGenerationFilters(List.of(
                        new PostGenerationFilterPairRules(PairRules.empty(), Map.of(0, rowPairRules)),
                        new PostGenerationFilterMinAppearanceCount(Map.of(0, minimums)),
                        new PostGenerationFilterBreakRules(Map.of(0, breakRules))
                ))
                .withScoreFilters(List.of(new ScoreFilterRowFullOrEmpty(1, 2, 3)))
                .withEquivalentRowPositions()
                .withStaticCandidateDomains()
                .withSearchDeadline(GeneratorSearchDeadline.after(Duration.ofSeconds(10)));

        Sheet generatedSheet = generator.generate();

        assertThat(generator.isSearchDeadlineReached()).isFalse();
        assertThat(generatedSheet.getSubSheets().getFirst().getCells())
                .allSatisfy(cell -> assertThat(cell.getCandidate()).isNotNull());
    }

    private static Set<Integer> rows(int from, int to) {
        return java.util.stream.IntStream.rangeClosed(from, to)
                .boxed()
                .collect(java.util.stream.Collectors.toSet());
    }

    private static BreakRule requiredBreak(int minimum, int maximum) {
        return new BreakRule(BreakRequirement.REQUIRE_BREAK, new IntRange(minimum, maximum));
    }

}
