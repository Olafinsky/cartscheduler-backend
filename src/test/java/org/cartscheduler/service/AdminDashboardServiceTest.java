package org.cartscheduler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.util.List;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.Proposal;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.repository.ProposalRepository;
import org.cartscheduler.repository.ScheduleDayRepository;
import org.cartscheduler.repository.ScheduleRepository;
import org.cartscheduler.web.model.DashboardSummary;
import org.cartscheduler.web.model.ProposalCellDisplay;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private ScheduleDayRepository scheduleDayRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private ProposalRepository proposalRepository;

    @InjectMocks
    private AdminDashboardService dashboardService;

    @Test
    void groupsAndFormatsParticipantProposalsForTheCurrentSchedule() {
        Schedule schedule = new Schedule();
        schedule.setId(7L);
        schedule.setName("Aktualny grafik");

        ScheduleDay day = new ScheduleDay();
        day.setId(11L);
        day.setSchedule(schedule);
        day.setName("Poniedziałek");
        day.setScheduleDate(LocalDate.now());
        day.setDayOfWeek((short) 1);

        Participant participant = new Participant();
        participant.setId(4L);
        participant.setName("Anna Kowalska");

        Participant pairParticipant = new Participant();
        pairParticipant.setId(5L);
        pairParticipant.setName("Władysław Szczekotowski");

        Proposal proposal = new Proposal();
        proposal.setId(9L);
        proposal.setParticipant(participant);
        proposal.setPairParticipant(pairParticipant);
        proposal.setScheduleDay(day);
        proposal.setHourStart((short) 11);
        proposal.setHourEnd((short) 11);
        proposal.setServiceLengthStart((short) 1);
        proposal.setServiceLengthEnd((short) 1);
        proposal.setAlternativeGroup("Qwertyuiop");

        given(scheduleRepository.findAll(any(Sort.class))).willReturn(List.of(schedule));
        given(scheduleDayRepository.findForSchedule(7L)).willReturn(List.of(day));
        given(participantRepository.findAll(any(Sort.class))).willReturn(List.of(participant));
        given(proposalRepository.findForSchedule(7L)).willReturn(List.of(proposal));

        DashboardSummary result = dashboardService.getSummary(null);

        ProposalCellDisplay display = result.getParticipants().getFirst()
                .getProposalsByDay().get(11L).getFirst();
        assertThat(result.getSelectedScheduleId()).isEqualTo(7L);
        assertThat(display.getHours()).isEqualTo("11:00-12:00");
        assertThat(display.getServiceLength()).isEqualTo("1h");
        assertThat(display.getAlternativeGroup()).isEqualTo("Qwert.");
        assertThat(display.getPairParticipantName()).isEqualTo("Władysław Szczek.");
    }

    @Test
    void defaultsToTheMostRecentlyCreatedScheduleInsteadOfTheOneContainingToday() {
        Schedule olderCurrentSchedule = new Schedule();
        olderCurrentSchedule.setId(1L);
        olderCurrentSchedule.setName("Grafik październik");

        Schedule newestSchedule = new Schedule();
        newestSchedule.setId(5L);
        newestSchedule.setName("Grafik listopad");

        ScheduleDay currentDay = new ScheduleDay();
        currentDay.setId(10L);
        currentDay.setSchedule(olderCurrentSchedule);
        currentDay.setName("Dzisiaj");
        currentDay.setScheduleDate(LocalDate.now());
        currentDay.setDayOfWeek((short) 3);

        ScheduleDay newestDay = new ScheduleDay();
        newestDay.setId(11L);
        newestDay.setSchedule(newestSchedule);
        newestDay.setName("Poniedziałek");
        newestDay.setScheduleDate(LocalDate.now().plusDays(5));
        newestDay.setDayOfWeek((short) 1);

        given(scheduleRepository.findAll(any(Sort.class))).willReturn(List.of(newestSchedule, olderCurrentSchedule));
        given(scheduleDayRepository.findForSchedule(olderCurrentSchedule.getId())).willReturn(List.of(currentDay));
        given(scheduleDayRepository.findForSchedule(newestSchedule.getId())).willReturn(List.of(newestDay));
        given(participantRepository.findAll(any(Sort.class))).willReturn(List.of());
        given(proposalRepository.findForSchedule(newestSchedule.getId())).willReturn(List.of());

        DashboardSummary result = dashboardService.getSummary(null);

        assertThat(result.getSelectedScheduleId()).isEqualTo(newestSchedule.getId());
        assertThat(result.getSelectedScheduleName()).isEqualTo("Grafik listopad");
    }
}
