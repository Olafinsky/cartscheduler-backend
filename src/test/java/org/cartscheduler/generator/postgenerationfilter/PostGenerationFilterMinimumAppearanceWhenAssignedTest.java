package org.cartscheduler.generator.postgenerationfilter;

import java.util.List;
import java.util.Map;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Generator;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostGenerationFilterMinimumAppearanceWhenAssignedTest {

    private final PostGenerationFilterMinimumAppearanceWhenAssigned filter =
            new PostGenerationFilterMinimumAppearanceWhenAssigned(Map.of(0, Map.of(1L, 2)));

    @Test
    void acceptsAnOmittedCandidateButRejectsAStartedServiceBelowTheMinimum() {
        Sheet sheet = new Sheet(List.of(new SubSheet(1, 2)));
        SubSheet day = sheet.getSubSheets().getFirst();

        assertThat(filter.isAccepted(sheet, day)).isTrue();

        day.getCell(0, 0).setCandidate(new Candidate(1L));
        assertThat(filter.isAccepted(sheet, day)).isFalse();

        day.getCell(0, 1).setCandidate(new Candidate(1L));
        assertThat(filter.isAccepted(sheet, day)).isTrue();
    }

    @Test
    void leavesCandidateOutWhenTheMinimumCannotBeReached() {
        Sheet generated = Generator.forSheet(new Sheet(List.of(new SubSheet(1, 1))))
                .withCandidates(List.of(new Candidate(1L)))
                .withPostGenerationFilters(List.of(filter))
                .generate();

        assertThat(generated.getSubSheets().getFirst().getCell(0, 0).getCandidate()).isNull();
    }
}
