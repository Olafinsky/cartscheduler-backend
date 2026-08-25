package org.cartscheduler.service;

import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.impl.RestUserDetails;
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
import static org.cartscheduler.support.TestEntityFactory.participant;
import static org.cartscheduler.support.TestEntityFactory.schedule;
import static org.cartscheduler.support.TestEntityFactory.user;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void shouldDenyAccessWhenScheduleDoesNotExist() {
        given(scheduleRepository.findById(99L)).willReturn(Optional.empty());

        boolean hasAccess = scheduleService.checkScheduleAccess(user(1L), 99L);

        assertThat(hasAccess).isFalse();
    }

    @Test
    void shouldGrantAccessToParticipantAssignedToSchedule() {
        Schedule schedule = new Schedule();
        schedule.setAccessibleParticipants(List.of(participant(1L)));
        given(scheduleRepository.findById(10L)).willReturn(Optional.of(schedule));

        boolean hasAccess = scheduleService.checkScheduleAccess(user(1L), 10L);

        assertThat(hasAccess).isTrue();
    }

    @Test
    void shouldDenyAccessToParticipantOutsideSchedule() {
        Schedule schedule = new Schedule();
        schedule.setAccessibleParticipants(List.of(participant(2L)));
        given(scheduleRepository.findById(10L)).willReturn(Optional.of(schedule));

        boolean hasAccess = scheduleService.checkScheduleAccess(user(1L), 10L);

        assertThat(hasAccess).isFalse();
    }

    @Test
    void shouldMapAllSchedulesAccessibleToParticipant() {
        given(scheduleRepository.findForParticipant(1L)).willReturn(List.of(
                schedule(10L, "Morning", List.of()),
                schedule(20L, "Evening", List.of())
        ));

        var schedules = scheduleService.prepareScheduleDtoListForParticipant(1L);

        assertThat(schedules)
                .extracting(dto -> dto.getId(), dto -> dto.getName())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10L, "Morning"),
                        org.assertj.core.groups.Tuple.tuple(20L, "Evening")
                );
    }

    @Test
    void shouldReturnTheRequestedScheduleInsteadOfTheFirstAccessibleOne() {
        given(scheduleRepository.findForParticipant(1L)).willReturn(List.of(
                schedule(10L, "First", List.of()),
                schedule(20L, "Requested", List.of())
        ));

        var result = scheduleService.prepareScheduleDtoForParticipant(1L, 20L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getName()).isEqualTo("Requested");
        verify(scheduleRepository).findForParticipant(1L);
    }

    @Test
    void shouldReturnNullWhenParticipantCannotAccessRequestedSchedule() {
        given(scheduleRepository.findForParticipant(1L)).willReturn(List.of(
                schedule(10L, "Only schedule", List.of())
        ));

        var result = scheduleService.prepareScheduleDtoForParticipant(1L, 20L);

        assertThat(result).isNull();
    }
}
