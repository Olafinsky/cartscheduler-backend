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

    private RestUserDetails user(long participantId) {
        return new RestUserDetails(participant(participantId));
    }

    private Participant participant(long id) {
        Participant participant = new Participant();
        participant.setId(id);
        participant.setName("Participant " + id);
        participant.setEmail("participant" + id + "@example.test");
        return participant;
    }
}
