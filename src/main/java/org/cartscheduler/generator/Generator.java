package org.cartscheduler.generator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Raw, database-independent assignment generator.
 *
 * <p>The search uses the most constrained remaining cell first. Scores and
 * complete post-generation filters are evaluated only after a full assignment
 * has been produced; partial filters can still reject impossible branches
 * early.</p>
 */
public class Generator {
    private static final int DEADLINE_CHECK_INTERVAL = 256;

    private Sheet sheet;
    private List<PostGenerationFilter> postGenerationFilters = List.of();
    private List<AcceptanceFilter> acceptanceFilters = List.of();
    private List<ScoreFilter> scoreFilters = List.of();
    private List<Candidate> candidates = List.of();
    private List<Candidate> activeCandidates = List.of();
    private GeneratorSearchDeadline searchDeadline = GeneratorSearchDeadline.unlimited();
    private GeneratorSearchDeadline activeSearchDeadline = GeneratorSearchDeadline.unlimited();
    private boolean equivalentRowPositions;
    private boolean staticCandidateDomains;
    private boolean searchDeadlineReached;
    private boolean activeSearchDeadlineReached;
    private long searchedNodes;

    private static final class BestResult {
        private SubSheet subSheet;
        private int score = Integer.MIN_VALUE;
        private int assignedCellCount = -1;
    }

    private Generator() {
    }

    public static Generator forSheet(Sheet sheet) {
        Generator instance = new Generator();
        instance.sheet = Objects.requireNonNull(sheet, "sheet must not be null").copy();
        return instance;
    }

    public Generator withPostGenerationFilters(List<PostGenerationFilter> postGenerationFilters) {
        this.postGenerationFilters = List.copyOf(
                Objects.requireNonNull(postGenerationFilters, "postGenerationFilters must not be null")
        );
        return this;
    }

    public Generator withAcceptanceFilters(List<AcceptanceFilter> acceptanceFilters) {
        this.acceptanceFilters = List.copyOf(
                Objects.requireNonNull(acceptanceFilters, "acceptanceFilters must not be null")
        );
        return this;
    }

    public Generator withScoreFilters(List<ScoreFilter> scoreFilters) {
        this.scoreFilters = List.copyOf(Objects.requireNonNull(scoreFilters, "scoreFilters must not be null"));
        return this;
    }

    public Generator withCandidates(List<Candidate> candidates) {
        this.candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates must not be null"));
        return this;
    }

    /**
     * Declares that the positions in a row are interchangeable. In that mode
     * the generator visits only the canonical order of candidates in a row
     * (ascending candidate id, followed by empty positions). It is useful for
     * schedules where participant_1 and participant_2 have no semantic order.
     */
    public Generator withEquivalentRowPositions() {
        this.equivalentRowPositions = true;
        return this;
    }

    /**
     * Allows the generator to remove candidates that are unavailable in every
     * initially free cell of a sub-sheet and to try narrow domains first. The
     * caller must use acceptance filters that only become more restrictive as
     * assignments are added.
     */
    public Generator withStaticCandidateDomains() {
        this.staticCandidateDomains = true;
        return this;
    }

    /**
     * Uses a total search-time budget. The remaining time is shared between
     * sub-sheets so that one difficult day cannot consume the entire budget.
     * If a valid result for a sub-sheet has already been found when its share
     * is reached, that best-so-far result is kept. Otherwise generation fails
     * explicitly.
     */
    public Generator withSearchDeadline(GeneratorSearchDeadline searchDeadline) {
        this.searchDeadline = Objects.requireNonNull(searchDeadline, "searchDeadline must not be null");
        return this;
    }

    /** Returns whether this generator stopped optimizing because of its deadline. */
    public boolean isSearchDeadlineReached() {
        return searchDeadlineReached;
    }

    public Sheet generate() {
        searchDeadlineReached = false;
        searchedNodes = 0;
        int remainingSubSheets = (int) sheet.getSubSheets().stream()
                .filter(subSheet -> !subSheet.getCells().isEmpty())
                .count();
        for (int i = 0; i < sheet.getSubSheets().size(); i++) {
            if (sheet.getSubSheets().get(i).getCells().isEmpty()) {
                continue;
            }
            activeSearchDeadline = searchDeadline.equalShareOfRemainingTime(remainingSubSheets);
            activeSearchDeadlineReached = false;
            generateSubSheet(i);
            remainingSubSheets--;
        }
        return sheet;
    }

    private void generateSubSheet(int subSheetIndex) {
        SubSheet subSheet = sheet.getSubSheets().get(subSheetIndex);
        if (subSheet.getCells().isEmpty()) {
            return;
        }

        BestResult best = new BestResult();
        Map<Cell, Boolean> assignedCells = new IdentityHashMap<>();
        for (Cell cell : subSheet.getCells()) {
            if (cell.isFixed()) {
                assignedCells.put(cell, Boolean.TRUE);
            }
        }
        activeCandidates = staticCandidateDomains ? candidatesForSubSheet(subSheet) : candidates;

        // A candidate submission is optional.  Start from the schedule that
        // uses no optional candidates, so an expired optimization deadline
        // can still return a valid schedule with holes instead of treating
        // unfilled cells as an error. Fixed cells are deliberately preserved.
        seedBestResultWithEmptyOptionalCells(subSheet, best);

        if (activeSearchDeadline.hasExpired()) {
            activeSearchDeadlineReached = true;
            searchDeadlineReached = true;
            if (best.subSheet == null) {
                throw new GenerationSearchLimitExceededException("The generator search deadline was reached");
            }
            sheet.getSubSheets().set(subSheetIndex, best.subSheet);
            return;
        }

        search(subSheet, assignedCells, best);

        if (best.subSheet == null) {
            if (activeSearchDeadlineReached) {
                throw new GenerationSearchLimitExceededException("The generator search deadline was reached");
            }
            throw new IllegalStateException("No assignment satisfies the configured generator constraints");
        }
        sheet.getSubSheets().set(subSheetIndex, best.subSheet);
    }

    private void seedBestResultWithEmptyOptionalCells(SubSheet subSheet, BestResult best) {
        for (Cell cell : subSheet.getCells()) {
            if (!cell.isFixed()) {
                cell.setCandidate(null);
            }
        }
        updateBestResult(subSheet, best);
    }

    private void search(SubSheet subSheet, Map<Cell, Boolean> assignedCells, BestResult best) {
        if (hasReachedSearchDeadline()) {
            return;
        }

        SearchContext context = new SearchContext(subSheet, assignedCells);
        if (!isPossible(context)) {
            return;
        }
        if (best.subSheet != null && cannotImproveBestScore(context, best)) {
            return;
        }

        if (context.unassignedCells().isEmpty()) {
            updateBestResult(subSheet, best);
            return;
        }

        RequiredCandidateChoice requiredCandidate = selectRequiredCandidate(context);
        if (requiredCandidate != null) {
            for (Cell cell : requiredCandidate.cells()) {
                cell.setCandidate(requiredCandidate.candidate());
                assignedCells.put(cell, Boolean.TRUE);
                search(subSheet, assignedCells, best);
                assignedCells.remove(cell);
                cell.setCandidate(null);
                if (activeSearchDeadlineReached) {
                    break;
                }
            }
            return;
        }

        SearchChoice choice = selectNextCell(context);
        for (Candidate candidate : choice.candidates()) {
            choice.cell().setCandidate(candidate);
            assignedCells.put(choice.cell(), Boolean.TRUE);
            search(subSheet, assignedCells, best);
            assignedCells.remove(choice.cell());
            if (activeSearchDeadlineReached) {
                break;
            }
        }
        choice.cell().setCandidate(null);
    }

    private SearchChoice selectNextCell(SearchContext context) {
        SubSheet subSheet = context.subSheet();
        SearchChoice bestChoice = null;
        for (Cell cell : context.unassignedCells()) {
            SearchChoice choice = new SearchChoice(cell, getAvailableCandidateOptions(context, cell));
            if (bestChoice == null || isBetterChoice(choice, bestChoice, subSheet, context.assignedCells)) {
                bestChoice = choice;
            }
        }
        return bestChoice;
    }

    /**
     * Before selecting an arbitrary cell, satisfy candidates that are known to
     * need further assignments. This turns dense schedules with many minimum
     * service lengths from a broad cell-by-cell search into a much narrower
     * constraint-first search.
     */
    private RequiredCandidateChoice selectRequiredCandidate(SearchContext context) {
        RequiredCandidateChoice bestChoice = null;
        for (Candidate candidate : context.candidates()) {
            int requiredAssignments = remainingRequiredAssignments(context, candidate);
            if (requiredAssignments == 0) {
                continue;
            }

            List<Cell> availableCells = new ArrayList<>();
            for (Cell cell : context.unassignedCells()) {
                if (context.canAssign(candidate, cell)) {
                    availableCells.add(cell);
                }
            }
            RequiredCandidateChoice choice = new RequiredCandidateChoice(
                    candidate,
                    requiredAssignments,
                    availableCells
            );
            if (bestChoice == null || isBetterRequiredCandidateChoice(choice, bestChoice, context)) {
                bestChoice = choice;
            }
        }
        return bestChoice;
    }

    private int remainingRequiredAssignments(GeneratorSearchContext context, Candidate candidate) {
        int requiredAssignments = 0;
        for (PostGenerationFilter filter : postGenerationFilters) {
            if (filter instanceof RequiredCandidateAssignmentFilter requiredFilter) {
                requiredAssignments = Math.max(
                        requiredAssignments,
                        requiredFilter.remainingRequiredAssignments(context, candidate)
                );
            }
        }
        return requiredAssignments;
    }

    private boolean isBetterRequiredCandidateChoice(
            RequiredCandidateChoice candidateChoice,
            RequiredCandidateChoice currentBest,
            SearchContext context
    ) {
        int candidateSlack = candidateChoice.cells().size() - candidateChoice.requiredAssignments();
        int currentSlack = currentBest.cells().size() - currentBest.requiredAssignments();
        if (candidateSlack != currentSlack) {
            return candidateSlack < currentSlack;
        }
        if (candidateChoice.requiredAssignments() != currentBest.requiredAssignments()) {
            return candidateChoice.requiredAssignments() > currentBest.requiredAssignments();
        }
        int candidatePriority = context.candidatePriority(candidateChoice.candidate());
        int currentPriority = context.candidatePriority(currentBest.candidate());
        if (candidatePriority != currentPriority) {
            return candidatePriority > currentPriority;
        }
        return candidateChoice.candidate().getId() < currentBest.candidate().getId();
    }

    private boolean isBetterChoice(
            SearchChoice candidateChoice,
            SearchChoice currentBest,
            SubSheet subSheet,
            Map<Cell, Boolean> assignedCells
    ) {
        int candidateOptionCount = candidateChoice.candidates().size();
        int currentOptionCount = currentBest.candidates().size();
        if (candidateOptionCount != currentOptionCount) {
            return candidateOptionCount < currentOptionCount;
        }

        boolean candidateCompletesRow = hasAssignedSibling(candidateChoice.cell(), subSheet, assignedCells);
        boolean currentCompletesRow = hasAssignedSibling(currentBest.cell(), subSheet, assignedCells);
        if (candidateCompletesRow != currentCompletesRow) {
            return candidateCompletesRow;
        }

        return Comparator.comparingInt(Cell::getY)
                .thenComparingInt(Cell::getX)
                .compare(candidateChoice.cell(), currentBest.cell()) < 0;
    }

    private boolean hasAssignedSibling(Cell cell, SubSheet subSheet, Map<Cell, Boolean> assignedCells) {
        for (int x = 0; x < subSheet.getSizeX(); x++) {
            Cell sibling = subSheet.getCell(x, cell.getY());
            if (sibling != cell && assignedCells.containsKey(sibling)) {
                return true;
            }
        }
        return false;
    }

    private List<Candidate> getAvailableCandidateOptions(SearchContext context, Cell cell) {
        List<Candidate> allowedCandidates = new ArrayList<>();
        for (Candidate candidate : context.candidates()) {
            if (context.canAssign(candidate, cell)) {
                allowedCandidates.add(candidate);
            }
        }
        allowedCandidates.sort(Comparator.comparingInt(context::candidatePriority).reversed());

        // A null assignment remains valid. It is tried after actual candidates
        // so that a usable result is found early and minimum constraints can
        // prune empty branches sooner.
        if (context.canLeaveEmpty(cell)) {
            allowedCandidates.add(null);
        }
        return allowedCandidates;
    }

    private boolean isAccepted(Candidate candidate, SubSheet subSheet, Cell cell) {
        for (AcceptanceFilter filter : acceptanceFilters) {
            if (!filter.isAccepted(candidate, sheet, subSheet, cell)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Candidates unavailable for every free cell of this sub-sheet can never
     * become available later when the caller has opted into static candidate
     * domains. Removing them per day avoids repeatedly asking availability
     * filters about unrelated participants. The remaining candidates are tried
     * from the narrowest domain first.
     */
    private List<Candidate> candidatesForSubSheet(SubSheet subSheet) {
        return candidates.stream()
                .map(candidate -> new CandidateAvailability(candidate, initialAvailableCellCount(subSheet, candidate)))
                .filter(candidate -> candidate.availableCellCount() > 0)
                .sorted(Comparator.comparingInt(CandidateAvailability::availableCellCount)
                        .thenComparingLong(candidate -> candidate.candidate().getId()))
                .map(CandidateAvailability::candidate)
                .toList();
    }

    private int initialAvailableCellCount(SubSheet subSheet, Candidate candidate) {
        int count = 0;
        for (Cell cell : subSheet.getCells()) {
            if (!cell.isFixed() && isAccepted(candidate, subSheet, cell)) {
                count++;
            }
        }
        return count;
    }

    private int candidatePriority(GeneratorSearchContext context, Candidate candidate) {
        int priority = 0;
        for (PostGenerationFilter filter : postGenerationFilters) {
            if (filter instanceof CandidateSelectionHeuristic heuristic) {
                priority += heuristic.priority(context, candidate);
            }
        }
        return priority;
    }

    private boolean cannotImproveBestScore(GeneratorSearchContext context, BestResult best) {
        long upperBound = 0;
        for (ScoreFilter filter : scoreFilters) {
            if (!(filter instanceof OptimisticScoreFilter optimisticFilter)) {
                return false;
            }
            upperBound += optimisticFilter.maximumPossibleScore(context);
        }
        return upperBound <= best.score;
    }

    private boolean isPossible(GeneratorSearchContext context) {
        for (PostGenerationFilter filter : postGenerationFilters) {
            if (filter instanceof PartialGenerationFilter partialFilter && !partialFilter.isPossible(context)) {
                return false;
            }
        }
        return true;
    }

    private void updateBestResult(SubSheet subSheet, BestResult best) {
        if (!isAllowedByPostGenerationFilters(subSheet)) {
            return;
        }

        int score = calculateScore(subSheet);
        int assignedCellCount = assignedCellCount(subSheet);
        if (score > best.score || (score == best.score && assignedCellCount > best.assignedCellCount)) {
            best.score = score;
            best.assignedCellCount = assignedCellCount;
            best.subSheet = subSheet.copy();
        }
    }

    private int assignedCellCount(SubSheet subSheet) {
        int count = 0;
        for (Cell cell : subSheet.getCells()) {
            if (cell.getCandidate() != null) {
                count++;
            }
        }
        return count;
    }

    private int calculateScore(SubSheet subSheet) {
        int score = 0;
        for (ScoreFilter filter : scoreFilters) {
            score += filter.calculateScore(sheet, subSheet);
        }
        return score;
    }

    private boolean isAllowedByPostGenerationFilters(SubSheet subSheet) {
        for (PostGenerationFilter filter : postGenerationFilters) {
            if (!filter.isAccepted(sheet, subSheet)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasReachedSearchDeadline() {
        searchedNodes++;
        if (!activeSearchDeadlineReached
                && searchedNodes % DEADLINE_CHECK_INTERVAL == 0
                && activeSearchDeadline.hasExpired()) {
            activeSearchDeadlineReached = true;
            searchDeadlineReached = true;
        }
        return activeSearchDeadlineReached;
    }

    private boolean keepsEquivalentRowOrder(
            SubSheet subSheet,
            Cell cell,
            Candidate candidate,
            Map<Cell, Boolean> assignedCells
    ) {
        if (!equivalentRowPositions) {
            return true;
        }

        for (int x = 0; x < subSheet.getSizeX(); x++) {
            if (x == cell.getX()) {
                continue;
            }
            Cell sibling = subSheet.getCell(x, cell.getY());
            if (!assignedCells.containsKey(sibling)) {
                continue;
            }
            Candidate siblingCandidate = sibling.getCandidate();
            if (x < cell.getX()) {
                if (siblingCandidate == null && candidate != null) {
                    return false;
                }
                if (siblingCandidate != null && candidate != null
                        && siblingCandidate.getId() >= candidate.getId()) {
                    return false;
                }
            } else {
                if (candidate == null && siblingCandidate != null) {
                    return false;
                }
                if (candidate != null && siblingCandidate != null
                        && candidate.getId() >= siblingCandidate.getId()) {
                    return false;
                }
            }
        }
        return true;
    }

    private final class SearchContext implements GeneratorSearchContext {
        private final SubSheet subSheet;
        private final Map<Cell, Boolean> assignedCells;
        private final List<Cell> unassignedCells;
        private final Map<Cell, Map<Candidate, Boolean>> acceptanceCache = new IdentityHashMap<>();
        private final Map<Candidate, Integer> candidatePriorityCache = new IdentityHashMap<>();

        private SearchContext(SubSheet subSheet, Map<Cell, Boolean> assignedCells) {
            this.subSheet = subSheet;
            this.assignedCells = assignedCells;
            this.unassignedCells = subSheet.getCells().stream()
                    .filter(cell -> !assignedCells.containsKey(cell))
                    .toList();
        }

        @Override
        public Sheet sheet() {
            return sheet;
        }

        @Override
        public SubSheet subSheet() {
            return subSheet;
        }

        @Override
        public List<Candidate> candidates() {
            return activeCandidates;
        }

        @Override
        public List<Cell> unassignedCells() {
            return unassignedCells;
        }

        @Override
        public boolean isUnassigned(Cell cell) {
            return !assignedCells.containsKey(cell);
        }

        @Override
        public boolean canAssign(Candidate candidate, Cell cell) {
            if (!isUnassigned(cell) || !keepsEquivalentRowOrder(subSheet, cell, candidate, assignedCells)) {
                return false;
            }
            return acceptanceCache
                    .computeIfAbsent(cell, ignored -> new IdentityHashMap<>())
                    .computeIfAbsent(candidate, ignored -> isAccepted(candidate, subSheet, cell));
        }

        private boolean canLeaveEmpty(Cell cell) {
            return isUnassigned(cell) && keepsEquivalentRowOrder(subSheet, cell, null, assignedCells);
        }

        private int candidatePriority(Candidate candidate) {
            return candidatePriorityCache.computeIfAbsent(candidate, ignored -> Generator.this.candidatePriority(this, candidate));
        }
    }

    private record SearchChoice(Cell cell, List<Candidate> candidates) {
    }

    private record RequiredCandidateChoice(Candidate candidate, int requiredAssignments, List<Cell> cells) {
    }

    private record CandidateAvailability(Candidate candidate, int availableCellCount) {
    }
}
