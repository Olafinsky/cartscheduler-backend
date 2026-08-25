package org.cartscheduler.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProposalServiceTest {

    private final ProposalService proposalService = new ProposalService();

    @Test
    void shouldExplicitlyReportThatListingProposalsIsNotImplemented() {
        assertThatThrownBy(() -> proposalService.prepareProposalDto(1L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_IMPLEMENTED));
    }

    @Test
    void shouldExplicitlyReportThatDeletingProposalIsNotImplemented() {
        assertThatThrownBy(() -> proposalService.delete(1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_IMPLEMENTED));
    }
}
