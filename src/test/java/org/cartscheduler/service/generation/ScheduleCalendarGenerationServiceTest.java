package org.cartscheduler.service.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.cartscheduler.entity.ConfigScheduleDay;
import org.cartscheduler.entity.ConfigScheduleRestrictedDate;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.ParticipantAccessToken;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.entity.ScheduleAccess;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.repository.ConfigScheduleDayRepository;
import org.cartscheduler.repository.ConfigScheduleRestrictedDateRepository;
import org.cartscheduler.repository.ParticipantAccessTokenRepository;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.repository.ScheduleAccessRepository;
import org.cartscheduler.repository.ScheduleDayRepository;
import org.cartscheduler.repository.ScheduleEntryRepository;
import org.cartscheduler.repository.ScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleCalendarGenerationServiceTest {
    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleDayRepository scheduleDayRepository;

    @Mock
    private ScheduleEntryRepository scheduleEntryRepository;

    @Mock
    private ConfigScheduleDayRepository configScheduleDayRepository;

    @Mock
    private ConfigScheduleRestrictedDateRepository restrictedDateRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private ScheduleAccessRepository scheduleAccessRepository;

    @Mock
    private ParticipantAccessTokenRepository participantAccessTokenRepository;

    @InjectMocks
    private ScheduleCalendarGenerationService scheduleCalendarGenerationService;

    @Test
    void createsFullWeeksUsingConfiguredHoursAndSkippingRestrictedDates() {
        ConfigScheduleRestrictedDate restrictedDate = new ConfigScheduleRestrictedDate();
        restrictedDate.setRestrictedDate(LocalDate.of(2026, 11, 9));
        given(scheduleRepository.existsByName("Grafik Listopad 2026")).willReturn(false);
        given(configScheduleDayRepository.findByActiveTrueOrderByDayOfWeekAsc()).willReturn(List.of(
                configDay((short) 1, (short) 7, (short) 8),
                configDay((short) 7, (short) 11, (short) 11)
        ));
        given(restrictedDateRepository.findByRestrictedDateBetween(
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 12, 6)
        )).willReturn(List.of(restrictedDate));
        given(participantRepository.findEnabledForNewScheduleAutomation()).willReturn(List.of());
        given(scheduleRepository.save(any(Schedule.class))).willAnswer(invocation -> {
            Schedule schedule = invocation.getArgument(0);
            schedule.setId(15L);
            return schedule;
        });

        ScheduleCalendarGenerationResult result = scheduleCalendarGenerationService.generate(
                LocalDate.of(2026, 10, 26)
        );

        assertThat(result).isEqualTo(new ScheduleCalendarGenerationResult(
                true,
                "Grafik Listopad 2026",
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 12, 6),
                9,
                13
        ));

        ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(scheduleCaptor.capture());
        Schedule savedSchedule = scheduleCaptor.getValue();
        assertThat(savedSchedule.getScheduleDays())
                .extracting(ScheduleDay::getName)
                .containsExactly(
                        "2026-11-02 (Poniedziałek)",
                        "2026-11-08 (Niedziela)",
                        "2026-11-15 (Niedziela)",
                        "2026-11-16 (Poniedziałek)",
                        "2026-11-22 (Niedziela)",
                        "2026-11-23 (Poniedziałek)",
                        "2026-11-29 (Niedziela)",
                        "2026-11-30 (Poniedziałek)",
                        "2026-12-06 (Niedziela)"
                );
        assertThat(savedSchedule.getScheduleDays())
                .extracting(ScheduleDay::getDayOfWeek)
                .containsExactly(
                        (short) 1,
                        (short) 7,
                        (short) 7,
                        (short) 1,
                        (short) 7,
                        (short) 1,
                        (short) 7,
                        (short) 1,
                        (short) 7
                );
        assertThat(savedSchedule.getScheduleDays())
                .extracting(ScheduleDay::getScheduleDate)
                .containsExactly(
                        LocalDate.of(2026, 11, 2),
                        LocalDate.of(2026, 11, 8),
                        LocalDate.of(2026, 11, 15),
                        LocalDate.of(2026, 11, 16),
                        LocalDate.of(2026, 11, 22),
                        LocalDate.of(2026, 11, 23),
                        LocalDate.of(2026, 11, 29),
                        LocalDate.of(2026, 11, 30),
                        LocalDate.of(2026, 12, 6)
                );
        assertThat(savedSchedule.getScheduleDays().getFirst().getScheduleEntries())
                .extracting(entry -> entry.getHour(), entry -> entry.getName())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple((short) 7, "07:00"),
                        org.assertj.core.groups.Tuple.tuple((short) 8, "08:00")
                );
        assertThat(savedSchedule.getScheduleDays().getLast().getScheduleEntries())
                .extracting(entry -> entry.getHour(), entry -> entry.getName())
                .containsExactly(org.assertj.core.groups.Tuple.tuple((short) 11, "11:00"));

        verify(scheduleDayRepository).saveAll(savedSchedule.getScheduleDays());
        verify(scheduleEntryRepository).saveAll(any());
        verifyNoInteractions(scheduleAccessRepository, participantAccessTokenRepository);
    }

    @Test
    void createsAccessesAndInvitationTokensForEnabledConfiguredParticipants() {
        Participant assignedAndInvited = participant(1L, true, true, true);
        Participant invitationOnly = participant(2L, true, false, true);
        Participant disabled = participant(3L, false, true, true);
        given(scheduleRepository.existsByName("Grafik Listopad 2026")).willReturn(false);
        given(configScheduleDayRepository.findByActiveTrueOrderByDayOfWeekAsc()).willReturn(List.of(
                configDay((short) 1, (short) 8, (short) 8)
        ));
        given(restrictedDateRepository.findByRestrictedDateBetween(
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 12, 6)
        )).willReturn(List.of());
        given(participantRepository.findEnabledForNewScheduleAutomation()).willReturn(List.of(
                assignedAndInvited,
                invitationOnly,
                disabled
        ));
        given(scheduleRepository.save(any(Schedule.class))).willAnswer(invocation -> {
            Schedule schedule = invocation.getArgument(0);
            schedule.setId(15L);
            return schedule;
        });
        Instant beforeGeneration = Instant.now();

        scheduleCalendarGenerationService.generate(LocalDate.of(2026, 10, 26));

        Instant afterGeneration = Instant.now();
        ArgumentCaptor<Iterable<ScheduleAccess>> accessCaptor = iterableCaptor();
        verify(scheduleAccessRepository).saveAll(accessCaptor.capture());
        assertThat(accessCaptor.getValue())
                .hasSize(1)
                .allSatisfy(access -> {
                    assertThat(access.getParticipant()).isSameAs(assignedAndInvited);
                    assertThat(access.getSchedule().getId()).isEqualTo(15L);
                });

        ArgumentCaptor<Iterable<ParticipantAccessToken>> tokenCaptor = iterableCaptor();
        verify(participantAccessTokenRepository).saveAll(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue())
                .hasSize(2)
                .allSatisfy(token -> {
                    assertThat(token.getParticipant())
                            .isIn(assignedAndInvited, invitationOnly);
                    assertThat(token.getSchedule().getId()).isEqualTo(15L);
                    assertThat(token.getToken()).matches("[A-Za-z0-9_-]{43}");
                    assertThat(token.getCreatedAt().toInstant()).isBetween(beforeGeneration, afterGeneration);
                    assertThat(token.getExpiresAt().toInstant()).isBetween(
                            beforeGeneration.plus(Duration.ofDays(7)),
                            afterGeneration.plus(Duration.ofDays(7))
                    );
                });
    }

    @Test
    void selectsTheClosestMonthWhoseFirstFullWeekHasNotPassed() {
        given(scheduleRepository.existsByName(any())).willReturn(true);

        ScheduleCalendarGenerationResult november = scheduleCalendarGenerationService.generate(
                LocalDate.of(2026, 10, 26)
        );
        ScheduleCalendarGenerationResult december = scheduleCalendarGenerationService.generate(
                LocalDate.of(2026, 12, 2)
        );
        ScheduleCalendarGenerationResult february = scheduleCalendarGenerationService.generate(
                LocalDate.of(2027, 1, 10)
        );

        assertThat(november)
                .extracting(
                        ScheduleCalendarGenerationResult::scheduleName,
                        ScheduleCalendarGenerationResult::startDate,
                        ScheduleCalendarGenerationResult::endDate
                )
                .containsExactly(
                        "Grafik Listopad 2026",
                        LocalDate.of(2026, 11, 2),
                        LocalDate.of(2026, 12, 6)
                );
        assertThat(december)
                .extracting(
                        ScheduleCalendarGenerationResult::scheduleName,
                        ScheduleCalendarGenerationResult::startDate,
                        ScheduleCalendarGenerationResult::endDate
                )
                .containsExactly(
                        "Grafik Grudzień 2026",
                        LocalDate.of(2026, 12, 7),
                        LocalDate.of(2027, 1, 3)
                );
        assertThat(february)
                .extracting(
                        ScheduleCalendarGenerationResult::scheduleName,
                        ScheduleCalendarGenerationResult::startDate,
                        ScheduleCalendarGenerationResult::endDate
                )
                .containsExactly(
                        "Grafik Luty 2027",
                        LocalDate.of(2027, 2, 1),
                        LocalDate.of(2027, 2, 28)
                );

        verifyNoInteractions(
                configScheduleDayRepository,
                restrictedDateRepository,
                participantRepository,
                scheduleAccessRepository,
                participantAccessTokenRepository,
                scheduleDayRepository,
                scheduleEntryRepository
        );
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void doesNotCreateTheSameCalendarScheduleTwice() {
        given(scheduleRepository.existsByName("Grafik Grudzień 2026")).willReturn(true);

        ScheduleCalendarGenerationResult result = scheduleCalendarGenerationService.generate(
                LocalDate.of(2026, 12, 2)
        );

        assertThat(result).isEqualTo(new ScheduleCalendarGenerationResult(
                false,
                "Grafik Grudzień 2026",
                LocalDate.of(2026, 12, 7),
                LocalDate.of(2027, 1, 3),
                0,
                0
        ));
        verifyNoInteractions(
                configScheduleDayRepository,
                restrictedDateRepository,
                participantRepository,
                scheduleAccessRepository,
                participantAccessTokenRepository,
                scheduleDayRepository,
                scheduleEntryRepository
        );
        verify(scheduleRepository, never()).save(any());
    }

    private ConfigScheduleDay configDay(short dayOfWeek, short hourFrom, short hourTo) {
        ConfigScheduleDay configDay = new ConfigScheduleDay();
        configDay.setDayOfWeek(dayOfWeek);
        configDay.setHourFrom(hourFrom);
        configDay.setHourTo(hourTo);
        configDay.setActive(true);
        return configDay;
    }

    private Participant participant(
            long id,
            boolean enabled,
            boolean autoassignmentEnabled,
            boolean autoinvitementEnabled
    ) {
        Participant participant = new Participant();
        participant.setId(id);
        participant.setName("Participant " + id);
        participant.setEmail("participant" + id + "@example.test");
        participant.setEnabled(enabled);
        participant.setNewScheduleAutoassignmentEnabled(autoassignmentEnabled);
        participant.setNewScheduleAutoinvitementEnabled(autoinvitementEnabled);
        return participant;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> ArgumentCaptor<Iterable<T>> iterableCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Iterable.class);
    }
}
