
package org.cartscheduler.generator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class Generator {

    private Sheet sheet;
    private List<PostGenerationFilter> postGenerationFilters = new ArrayList<>();
    private List<AcceptanceFilter> acceptanceFilters = new ArrayList<>();
    private List<ScoreFilter> scoreFilters = new ArrayList<>();
    private List<Candidate> candidates = new ArrayList<>();

    private static final class BestResult {
        private SubSheet subSheet;
        private int score = Integer.MIN_VALUE;
    }

    private Generator() {}

    public static Generator forSheet(Sheet sheet) {
        Generator instance = new Generator();
        instance.sheet = sheet.copy();

        return instance;
    }

    public Generator withPostGenerationFilters(List<PostGenerationFilter> postGenerationFilters) {
        this.postGenerationFilters = postGenerationFilters;

        return this;
    }


    public Generator withAcceptanceFilters(List<AcceptanceFilter> acceptanceFilters) {
        this.acceptanceFilters = acceptanceFilters;

        return this;
    }

    public Generator withScoreFilters(List<ScoreFilter> scoreFilters) {
        this.scoreFilters = scoreFilters;

        return this;
    }

    public Generator withCandidates(List<Candidate> candidates) {
        this.candidates = candidates;

        return this;
    }

    public Sheet generate() {
        for (int i = 0; i < sheet.getSubSheets().size(); i++) {
            generateSubSheet(i);
        }

        return sheet;
    }

    private void generateSubSheet(int subSheetIndex) {

        SubSheet subSheet = sheet.getSubSheets().get(subSheetIndex);
        int cellCount = subSheet.getCells().size();
        if (cellCount == 0) {
            return;
        }
        int caret = cellCount - 1;

        LinkedHashMap<Integer, List<Candidate>> candidateMap = new LinkedHashMap<>();
        LinkedHashMap<Integer, Integer> candidateChoices = new LinkedHashMap<>();

        boolean isInitialRun = true;
        boolean isCompleted = false;

        BestResult best = new BestResult();

        do {
            // Initial run - finding candidates for cell and setting first if available
            if (isInitialRun) {
                for (int a = 0; a < cellCount; a++) {
                    Cell cell = subSheet.getCells().get(a);
                    List<Candidate> options = cell.isFixed()
                        ? List.of(cell.getCandidate())
                        : getAvailableCandidateOptions(sheet, subSheet, cell);

                    candidateMap.put(a, options);
                    candidateChoices.put(a, 0);

                    setCandidateAndUpdateBest(subSheet, a, options.getFirst(), best);
                }

                isInitialRun = false;
                continue;
            }

            Integer tempCaret = null;

            for (int i = 0; i <= cellCount; i++) {

                boolean isCellUnprocessable = false;

                // This cell isn't expected do be processed
                if (caret != i) {
                    continue;
                }

                // Candidate list empty - cell cannot be processed
                if (candidateMap.get(i).isEmpty()) {
                    isCellUnprocessable = true;
                }

                // Selected candidate is the last one - cell cannot be processed
                else if (candidateChoices.get(i) >= (candidateMap.get(i).size() - 1)) {
                    isCellUnprocessable = true;
                }

                // No objections - selecting another candidate
                else {
                    clearCandidatesAfter(subSheet, i, best);

                    int newCandidateIndex = candidateChoices.get(i) + 1;
                    candidateChoices.put(i, newCandidateIndex);

                    Candidate candidate = candidateMap.get(i).get(newCandidateIndex);
                    setCandidateAndUpdateBest(subSheet, i, candidate, best);

                    // Candidate changed - next cells have to be reset
                    for (int j = i + 1; j < cellCount; j++) {
                        Cell cell = subSheet.getCells().get(j);
                        List<Candidate> options = cell.isFixed()
                            ? List.of(cell.getCandidate())
                            : getAvailableCandidateOptions(sheet, subSheet, cell);

                        candidateMap.put(j, options);
                        candidateChoices.put(j, 0);

                        candidate = options.getFirst();
                        setCandidateAndUpdateBest(subSheet, j, candidate, best);
                    }

                    // Resetting caret
                    tempCaret = cellCount - 1;
                }

                if (isCellUnprocessable) {
                    if (i == 0) {
                        isCompleted = true;
                        break;
                    }
                    tempCaret = i - 1;
                }
            }

            if (tempCaret != null) {
                caret = tempCaret;
            }

        } while (!isCompleted);

        if (best.subSheet == null) {
            throw new IllegalStateException("No assignment satisfies the configured generator constraints");
        }
        sheet.getSubSheets().set(subSheetIndex, best.subSheet);
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

    private List<Candidate> getAvailableCandidateOptions(Sheet sheet, SubSheet subSheet, Cell cell) {
        List<Candidate> allowedCandidates = new ArrayList<>();

        for (Candidate candidate : candidates) {
            boolean isAccepted = true;

            for (AcceptanceFilter filter : acceptanceFilters) {
                if (!filter.isAccepted(candidate, sheet, subSheet, cell)) {
                    isAccepted = false;
                    break;
                }
            }

            if (isAccepted) {
                allowedCandidates.add(candidate);
            }
        }

        List<Candidate> options = new ArrayList<>();
        options.add(null);
        options.addAll(allowedCandidates);

        return options;
    }

    private void setCandidateAndUpdateBest(SubSheet subSheet, int cellIndex, Candidate candidate, BestResult best) {
        subSheet.getCells().get(cellIndex).setCandidate(candidate);

        if (!isAllowedByPostGenerationFilters(subSheet)) return;

        int score = calculateScore(subSheet);
        if (score > best.score) {
            best.score = score;
            best.subSheet = subSheet.copy();
        }
    }

    private void clearCandidatesAfter(SubSheet subSheet, int cellIndex, BestResult best) {
        for (int i = cellIndex + 1; i < subSheet.getCells().size(); i++) {
            if (!subSheet.getCells().get(i).isFixed()) {
                setCandidateAndUpdateBest(subSheet, i, null, best);
            }
        }
    }
}

