package org.cartscheduler.service;

import org.cartscheduler.entity.Proposal;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.repository.ProposalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.cartscheduler.support.TestEntityFactory.participant;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    @Mock
    private ProposalRepository proposalRepository;

    @InjectMocks
    private ProposalService proposalService;

    @Test
    void shouldMapProposalsAssignedToParticipantForRequestedScheduleDay() {
        Proposal proposal = proposal(10L, 1L, 2L, 3L, 5L);
        given(proposalRepository.findForParticipantAndScheduleDay(1L, 5L)).willReturn(List.of(proposal));

        var result = proposalService.prepareProposalDto(1L, 5L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst())
                .extracting(
                        dto -> dto.getId(),
                        dto -> dto.getParticipantId(),
                        dto -> dto.getPairParticipantId(),
                        dto -> dto.getInsertingParticipantId(),
                        dto -> dto.getScheduleDayId(),
                        dto -> dto.getHourStart(),
                        dto -> dto.getHourEnd(),
                        dto -> dto.getBreakLengthStart(),
                        dto -> dto.getBreakLengthEnd(),
                        dto -> dto.getServiceLengthStart(),
                        dto -> dto.getServiceLengthEnd()
                )
                .containsExactly(10L, 1L, 2L, 3L, 5L, 8, 12, (short) 1, (short) 2, (short) 3, (short) 4);
        verify(proposalRepository).findForParticipantAndScheduleDay(1L, 5L);
    }

    @Test
    void shouldReturnEmptyListWhenParticipantHasNoProposalForScheduleDay() {
        given(proposalRepository.findForParticipantAndScheduleDay(1L, 5L)).willReturn(List.of());

        var result = proposalService.prepareProposalDto(1L, 5L);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldExplicitlyReportThatDeletingProposalIsNotImplemented() {
        assertThatThrownBy(() -> proposalService.delete(1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_IMPLEMENTED));
    }

    private Proposal proposal(long id, long participantId, long pairParticipantId, long insertingParticipantId, long scheduleDayId) {
        ScheduleDay scheduleDay = new ScheduleDay();
        scheduleDay.setId(scheduleDayId);

        Proposal proposal = new Proposal();
        proposal.setId(id);
        proposal.setDateAdd(Date.from(Instant.parse("2026-08-26T10:00:00Z")));
        proposal.setParticipant(participant(participantId));
        proposal.setPairParticipant(participant(pairParticipantId));
        proposal.setInsertingParticipant(participant(insertingParticipantId));
        proposal.setScheduleDay(scheduleDay);
        proposal.setHourStart((short) 8);
        proposal.setHourEnd((short) 12);
        proposal.setBreakLengthStart((short) 1);
        proposal.setBreakLengthEnd((short) 2);
        proposal.setServiceLengthStart((short) 3);
        proposal.setServiceLengthEnd((short) 4);
        return proposal;
    }
}
