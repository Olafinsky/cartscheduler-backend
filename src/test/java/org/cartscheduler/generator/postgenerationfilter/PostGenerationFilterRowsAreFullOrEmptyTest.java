package org.cartscheduler.generator.postgenerationfilter;

import java.util.List;
import org.cartscheduler.generator.Candidate;
import org.cartscheduler.generator.Generator;
import org.cartscheduler.generator.Sheet;
import org.cartscheduler.generator.SubSheet;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostGenerationFilterRowsAreFullOrEmptyTest {

    private final PostGenerationFilterRowsAreFullOrEmpty filter = new PostGenerationFilterRowsAreFullOrEmpty();

    @Test
    void acceptsAnEmptyOrFullRowButRejectsASingleParticipant() {
        Sheet sheet = new Sheet(List.of(new SubSheet(2, 1)));
        SubSheet day = sheet.getSubSheets().getFirst();

        assertThat(filter.isAccepted(sheet, day)).isTrue();

        day.getCell(0, 0).setCandidate(new Candidate(1L));
        assertThat(filter.isAccepted(sheet, day)).isFalse();

        day.getCell(1, 0).setCandidate(new Candidate(2L));
        assertThat(filter.isAccepted(sheet, day)).isTrue();
    }

    @Test
    void leavesTheRowEmptyWhenOnlyOneCandidateIsAvailable() {
        Sheet generated = Generator.forSheet(new Sheet(List.of(new SubSheet(2, 1))))
                .withCandidates(List.of(new Candidate(1L)))
                .withPostGenerationFilters(List.of(filter))
                .generate();

        assertThat(generated.getSubSheets().getFirst().getCells())
                .allSatisfy(cell -> assertThat(cell.getCandidate()).isNull());
    }
}
