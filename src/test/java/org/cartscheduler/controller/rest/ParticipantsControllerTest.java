package org.cartscheduler.controller.rest;

import org.cartscheduler.dto.rest.response.ParticipantDto;
import org.cartscheduler.impl.RestUserDetails;
import org.cartscheduler.service.ParticipantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cartscheduler.support.TestEntityFactory.user;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ParticipantsControllerTest {

    @Mock
    private ParticipantService participantService;

    @InjectMocks
    private ParticipantsController participantsController;

    @Test
    void shouldReturnParticipantsAssignedToAuthenticatedAgentInTokenSchedule() {
        RestUserDetails principal = user(1L);
        principal.setScheduleId(5L);
        List<ParticipantDto> expected = List.of(new ParticipantDto(2L, "Participant 2"));
        given(participantService.prepareAssignedParticipantDtoListForAgentParticipantAndSchedule(1L, 5L))
                .willReturn(expected);

        var result = participantsController.index(principal);

        assertThat(result).isSameAs(expected);
        verify(participantService).prepareAssignedParticipantDtoListForAgentParticipantAndSchedule(1L, 5L);
    }
}
