package org.cartscheduler.controller.rest;

import org.cartscheduler.dto.rest.response.ProposalDto;
import org.cartscheduler.impl.RestUserDetails;
import org.cartscheduler.service.ProposalService;
import org.cartscheduler.service.ScheduleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.cartscheduler.support.TestEntityFactory.user;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ProposalsControllerTest {

    @Mock
    private ProposalService proposalService;

    @Mock
    private ScheduleService scheduleService;

    @InjectMocks
    private ProposalsController proposalsController;

    @Test
    void shouldDelegateProposalListingWhenPrincipalHasAccessToTokenSchedule() {
        RestUserDetails principal = user(1L);
        principal.setScheduleId(5L);
        List<ProposalDto> expected = List.of();
        given(scheduleService.checkScheduleAccess(principal, 5L)).willReturn(true);
        given(proposalService.prepareProposalDto(2L, 10L)).willReturn(expected);

        var result = proposalsController.index(principal, 10L, 2L);

        assertThat(result).isSameAs(expected);
        verify(proposalService).prepareProposalDto(2L, 10L);
    }

    @Test
    void shouldForbidProposalListingWhenPrincipalHasNoScheduleAccess() {
        RestUserDetails principal = user(1L);
        principal.setScheduleId(5L);
        given(scheduleService.checkScheduleAccess(principal, 5L)).willReturn(false);

        assertThatThrownBy(() -> proposalsController.index(principal, 10L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(proposalService);
    }

    @Test
    void shouldDelegateProposalDeletionWhenPrincipalHasAccessToTokenSchedule() {
        RestUserDetails principal = user(1L);
        principal.setScheduleId(5L);
        given(scheduleService.checkScheduleAccess(principal, 5L)).willReturn(true);

        proposalsController.delete(principal, 10L);

        verify(proposalService).delete(10L);
    }
}
