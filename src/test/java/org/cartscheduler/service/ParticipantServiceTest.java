package org.cartscheduler.service;

import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.repository.ProposalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cartscheduler.support.TestEntityFactory.participant;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ParticipantServiceTest {

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private ProposalRepository proposalRepository;

    @InjectMocks
    private ParticipantService participantService;

    @Test
    void shouldMapParticipantsAssignedToAgentWithinSchedule() {
        given(participantRepository.findAssignedParticipants(1L, 5L)).willReturn(List.of(
                participant(2L),
                participant(3L)
        ));

        var result = participantService.prepareAssignedParticipantDtoListForAgentParticipantAndSchedule(1L, 5L);

        assertThat(result)
                .extracting(dto -> dto.getId(), dto -> dto.getName())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2L, "Participant 2"),
                        org.assertj.core.groups.Tuple.tuple(3L, "Participant 3")
                );
        verify(participantRepository).findAssignedParticipants(1L, 5L);
    }

    @Test
    void shouldReturnEmptyListWhenAgentHasNoAssignedParticipants() {
        given(participantRepository.findAssignedParticipants(1L, 5L)).willReturn(List.of());

        var result = participantService.prepareAssignedParticipantDtoListForAgentParticipantAndSchedule(1L, 5L);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldMapProposalStatusInOneBatchForAssignedParticipantsOnly() {
        given(participantRepository.findAssignedParticipants(1L, 5L)).willReturn(List.of(
                participant(2L),
                participant(3L),
                participant(4L)
        ));
        given(proposalRepository.findParticipantIdsWithProposalsForSchedule(5L, Set.of(2L, 3L, 4L)))
                .willReturn(List.of(2L));

        var result = participantService
                .prepareAssignedParticipantDtoListWithProposalStatusForAgentParticipantAndSchedule(1L, 5L);

        assertThat(result)
                .extracting(dto -> dto.getId(), dto -> dto.getHasProposals())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2L, true),
                        org.assertj.core.groups.Tuple.tuple(3L, false),
                        org.assertj.core.groups.Tuple.tuple(4L, false)
                );
        verify(proposalRepository).findParticipantIdsWithProposalsForSchedule(5L, Set.of(2L, 3L, 4L));
    }

    @Test
    void shouldNotQueryProposalStatusesWhenAgentHasNoAssignedParticipants() {
        given(participantRepository.findAssignedParticipants(1L, 5L)).willReturn(List.of());

        var result = participantService
                .prepareAssignedParticipantDtoListWithProposalStatusForAgentParticipantAndSchedule(1L, 5L);

        assertThat(result).isEmpty();
        verifyNoInteractions(proposalRepository);
    }
}
