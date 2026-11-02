package org.cartscheduler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.entity.ScheduleAccess;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.repository.ScheduleAccessRepository;
import org.cartscheduler.repository.ScheduleDayRepository;
import org.cartscheduler.repository.ScheduleRepository;
import org.cartscheduler.web.form.ScheduleForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleDayRepository scheduleDayRepository;

    @Mock
    private ScheduleAccessRepository scheduleAccessRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @InjectMocks
    private AdminScheduleService scheduleService;

    @Test
    void updatesScheduleNameAndReplacesParticipantAccess() {
        Schedule schedule = schedule(17L, "Stary grafik");
        Participant anna = participant(3L, "Anna Kowalska");
        Participant jan = participant(8L, "Jan Nowak");
        ScheduleForm form = new ScheduleForm();
        form.setName("  Grafik listopadowy  ");
        form.setParticipantIds(Set.of(3L, 8L));
        given(scheduleRepository.findById(17L)).willReturn(Optional.of(schedule));
        given(participantRepository.findAllById(Set.of(3L, 8L))).willReturn(List.of(anna, jan));

        scheduleService.update(17L, form);

        assertThat(schedule.getName()).isEqualTo("Grafik listopadowy");
        verify(scheduleRepository).save(schedule);
        verify(scheduleAccessRepository).deleteBySchedule_Id(17L);
        ArgumentCaptor<Iterable<ScheduleAccess>> accessCaptor = iterableCaptor();
        verify(scheduleAccessRepository).saveAll(accessCaptor.capture());
        assertThat(accessCaptor.getValue())
                .extracting(access -> access.getParticipant().getId())
                .containsExactlyInAnyOrder(3L, 8L);
        assertThat(accessCaptor.getValue())
                .allSatisfy(access -> assertThat(access.getSchedule()).isSameAs(schedule));
    }

    @Test
    void rejectsMissingParticipantBeforeRemovingExistingAccess() {
        Schedule schedule = schedule(17L, "Grafik");
        ScheduleForm form = new ScheduleForm();
        form.setName("Grafik");
        form.setParticipantIds(Set.of(3L));
        given(scheduleRepository.findById(17L)).willReturn(Optional.of(schedule));
        given(participantRepository.findAllById(Set.of(3L))).willReturn(List.of());

        assertThatThrownBy(() -> scheduleService.update(17L, form))
                .isInstanceOf(FieldValidationException.class)
                .hasMessage("Wybrano uczestnika, który już nie istnieje.");

        verify(scheduleAccessRepository, never()).deleteBySchedule_Id(17L);
        verify(scheduleAccessRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Iterable<ScheduleAccess>> iterableCaptor() {
        return ArgumentCaptor.forClass((Class) Iterable.class);
    }

    private Schedule schedule(long id, String name) {
        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setName(name);
        return schedule;
    }

    private Participant participant(long id, String name) {
        Participant participant = new Participant();
        participant.setId(id);
        participant.setName(name);
        return participant;
    }
}
