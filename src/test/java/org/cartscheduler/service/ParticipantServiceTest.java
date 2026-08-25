package org.cartscheduler.service;

import org.cartscheduler.repository.ParticipantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cartscheduler.support.TestEntityFactory.participant;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ParticipantServiceTest {

    @Mock
    private ParticipantRepository participantRepository;

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
}
