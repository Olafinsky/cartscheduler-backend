package org.cartscheduler.service;

import org.cartscheduler.entity.Schedule;
import org.cartscheduler.impl.RestUserDetails;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.repository.ScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.cartscheduler.support.TestEntityFactory.schedule;
import static org.cartscheduler.support.TestEntityFactory.user;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void shouldDenyAccessWhenScheduleDoesNotExist() {
        given(participantRepository.countAssignedParticipantsForAgentAndSchedule(1L, 99L)).willReturn(0L);

        boolean hasAccess = scheduleService.checkScheduleAccess(user(1L), 99L);

        assertThat(hasAccess).isFalse();
    }

    @Test
    void shouldGrantAccessWhenAgentHasParticipantAssignedToSchedule() {
        given(participantRepository.countAssignedParticipantsForAgentAndSchedule(1L, 10L)).willReturn(1L);

        boolean hasAccess = scheduleService.checkScheduleAccess(user(1L), 10L);

        assertThat(hasAccess).isTrue();
    }

    @Test
    void shouldDenyAccessWhenUserIsNotAgentOfParticipantAssignedToSchedule() {
        given(participantRepository.countAssignedParticipantsForAgentAndSchedule(1L, 10L)).willReturn(0L);

        boolean hasAccess = scheduleService.checkScheduleAccess(user(1L), 10L);

        assertThat(hasAccess).isFalse();
    }

    @Test
    void shouldMapAllSchedulesAccessibleThroughAssignedParticipants() {
        given(scheduleRepository.findForAgent(1L)).willReturn(List.of(
                schedule(10L, "Morning", List.of()),
                schedule(20L, "Evening", List.of())
        ));

        var schedules = scheduleService.prepareScheduleDtoListForAgentParticipant(1L);

        assertThat(schedules)
                .extracting(dto -> dto.getId(), dto -> dto.getName())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10L, "Morning"),
                        org.assertj.core.groups.Tuple.tuple(20L, "Evening")
                );
    }

    @Test
    void shouldReturnRequestedSchedule() {
        given(scheduleRepository.findById(20L)).willReturn(Optional.of(
                schedule(20L, "Requested", List.of())
        ));

        var result = scheduleService.prepareScheduleDtoForSchedule(20L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getName()).isEqualTo("Requested");
        verify(scheduleRepository).findById(20L);
    }

    @Test
    void shouldReturnNullWhenScheduleDoesNotExist() {
        given(scheduleRepository.findById(20L)).willReturn(Optional.empty());

        var result = scheduleService.prepareScheduleDtoForSchedule(20L);

        assertThat(result).isNull();
    }
}
