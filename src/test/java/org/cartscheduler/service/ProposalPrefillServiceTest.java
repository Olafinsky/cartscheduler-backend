package org.cartscheduler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.cartscheduler.dto.rest.response.ProposalPrefillDto;
import org.cartscheduler.entity.BreakMode;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.Proposal;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.repository.ProposalRepository;
import org.cartscheduler.repository.ScheduleDayRepository;
import org.cartscheduler.repository.ScheduleEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ProposalPrefillServiceTest {
    private static final long AGENT_ID = 1L;
    private static final long PARTICIPANT_ID = 2L;
    private static final long TARGET_SCHEDULE_ID = 100L;
    private static final long TARGET_DAY_ID = 200L;
    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 11, 2);

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private ScheduleDayRepository scheduleDayRepository;

    @Mock
    private ScheduleEntryRepository scheduleEntryRepository;

    @Mock
    private ParticipantService participantService;

    @InjectMocks
    private ProposalPrefillService proposalPrefillService;

    @Test
    void prefersNearestSameWeekdayInTargetScheduleAndIgnoresTargetFutureAndOtherWeekdays() {
        ScheduleDay targetDay = scheduleDay(TARGET_DAY_ID, TARGET_SCHEDULE_ID, TARGET_DATE);
        ScheduleDay sunday = scheduleDay(11L, TARGET_SCHEDULE_ID, LocalDate.of(2026, 11, 1));
        ScheduleDay olderMonday = scheduleDay(12L, TARGET_SCHEDULE_ID, LocalDate.of(2026, 10, 19));
        ScheduleDay nearestMonday = scheduleDay(13L, TARGET_SCHEDULE_ID, LocalDate.of(2026, 10, 26));
        ScheduleDay targetMonday = scheduleDay(14L, TARGET_SCHEDULE_ID, TARGET_DATE);
        ScheduleDay futureMonday = scheduleDay(15L, TARGET_SCHEDULE_ID, LocalDate.of(2026, 11, 9));
        Proposal source = proposal(50L, 11, 11);
        stubAuthorizedTarget(targetDay, participantWithPermissions(false, false));
        given(proposalRepository.findPrefillSourceDaysInSchedule(
                PARTICIPANT_ID, AGENT_ID, TARGET_DATE, TARGET_SCHEDULE_ID
        )).willReturn(List.of(sunday, olderMonday, futureMonday, targetMonday, nearestMonday));
        stubSelectedSource(nearestMonday, List.of(source), 0, List.of((short) 11));

        ProposalPrefillDto result = prefill();

        assertThat(result.isAvailable()).isTrue();
        assertThat(result.getSource())
                .extracting(sourceDto -> sourceDto.getScheduleId(), sourceDto -> sourceDto.getScheduleDayId(),
                        sourceDto -> sourceDto.getDate(), sourceDto -> sourceDto.getDayOfWeek())
                .containsExactly(TARGET_SCHEDULE_ID, 13L, LocalDate.of(2026, 10, 26), java.time.DayOfWeek.MONDAY);
        verify(proposalRepository, never()).findPrefillSourceDaysInEarlierSchedules(any(), any(), any(), any());
        verify(proposalRepository, never()).save(any());
        verify(proposalRepository, never()).saveAndFlush(any());
        verify(proposalRepository, never()).delete(any());
    }

    @Test
    void fallsBackToNearestEarlierAccessibleScheduleWhenCurrentScheduleHasNoSource() {
        ScheduleDay targetDay = scheduleDay(TARGET_DAY_ID, TARGET_SCHEDULE_ID, TARGET_DATE);
        ScheduleDay olderMonday = scheduleDay(31L, 80L, LocalDate.of(2026, 10, 19));
        ScheduleDay nearestMonday = scheduleDay(32L, 90L, LocalDate.of(2026, 10, 26));
        Proposal source = proposal(51L, 11, 11);
        stubAuthorizedTarget(targetDay, participantWithPermissions(false, false));
        given(proposalRepository.findPrefillSourceDaysInSchedule(
                PARTICIPANT_ID, AGENT_ID, TARGET_DATE, TARGET_SCHEDULE_ID
        )).willReturn(List.of());
        given(proposalRepository.findPrefillSourceDaysInEarlierSchedules(
                PARTICIPANT_ID, AGENT_ID, TARGET_DATE, TARGET_SCHEDULE_ID
        )).willReturn(List.of(olderMonday, nearestMonday));
        stubSelectedSource(nearestMonday, List.of(source), 0, List.of((short) 11));

        ProposalPrefillDto result = prefill();

        assertThat(result.isAvailable()).isTrue();
        assertThat(result.getSource().getScheduleId()).isEqualTo(90L);
        assertThat(result.getSource().getScheduleDayId()).isEqualTo(32L);
    }

    @Test
    void returnsUnavailableWhenThereIsNoAuthorizedHistoricalIdentityMatch() {
        ScheduleDay targetDay = scheduleDay(TARGET_DAY_ID, TARGET_SCHEDULE_ID, TARGET_DATE);
        stubAuthorizedTarget(targetDay, participantWithPermissions(false, false));
        given(proposalRepository.findPrefillSourceDaysInSchedule(
                PARTICIPANT_ID, AGENT_ID, TARGET_DATE, TARGET_SCHEDULE_ID
        )).willReturn(List.of());
        given(proposalRepository.findPrefillSourceDaysInEarlierSchedules(
                PARTICIPANT_ID, AGENT_ID, TARGET_DATE, TARGET_SCHEDULE_ID
        )).willReturn(List.of());

        ProposalPrefillDto result = prefill();

        assertThat(result)
                .extracting(ProposalPrefillDto::isAvailable, ProposalPrefillDto::getSource, ProposalPrefillDto::getValues)
                .containsExactly(false, null, null);
        verify(proposalRepository, never()).findForParticipantAndScheduleDayForPrefill(any(), any());
        verify(proposalRepository, never()).countByParticipant_IdAndScheduleDay_Id(any(), any());
    }

    @Test
    void selectsFirstSortedSourceProposalWhenTargetHasNoProposals() {
        ProposalPrefillDto result = prefillWithSourceProposalCount(0L);

        assertThat(result.getSource())
                .extracting(source -> source.getProposalId(), source -> source.getProposalIndex(), source -> source.getProposalCount())
                .containsExactly(61L, 0, 2);
    }

    @Test
    void selectsSecondSortedSourceProposalWhenTargetHasOneProposal() {
        ProposalPrefillDto result = prefillWithSourceProposalCount(1L);

        assertThat(result.getSource())
                .extracting(source -> source.getProposalId(), source -> source.getProposalIndex(), source -> source.getProposalCount())
                .containsExactly(62L, 1, 2);
    }

    @Test
    void keepsLastSortedSourceProposalWhenTargetHasAtLeastAsManyProposals() {
        ProposalPrefillDto result = prefillWithSourceProposalCount(4L);

        assertThat(result.getSource())
                .extracting(source -> source.getProposalId(), source -> source.getProposalIndex(), source -> source.getProposalCount())
                .containsExactly(62L, 1, 2);
    }

    @Test
    void sortsSourceProposalsByStartEndAndIdDeterministically() {
        ScheduleDay targetDay = scheduleDay(TARGET_DAY_ID, TARGET_SCHEDULE_ID, TARGET_DATE);
        ScheduleDay sourceDay = scheduleDay(41L, TARGET_SCHEDULE_ID, LocalDate.of(2026, 10, 26));
        Proposal later = proposal(80L, 15, 15);
        Proposal tiedWithHigherId = proposal(70L, 11, 12);
        Proposal first = proposal(60L, 11, 12);
        stubAuthorizedTarget(targetDay, participantWithPermissions(false, false));
        stubCurrentScheduleSourceDay(sourceDay);
        stubSelectedSource(
                sourceDay,
                List.of(later, tiedWithHigherId, first),
                0,
                List.of((short) 11, (short) 12, (short) 15)
        );

        ProposalPrefillDto result = prefill();

        assertThat(result.getSource())
                .extracting(source -> source.getProposalId(), source -> source.getProposalIndex(), source -> source.getProposalCount())
                .containsExactly(60L, 0, 3);
    }

    @Test
    void mapsAllowedAlternativeGroupAndPairByTheGlobalParticipantIdentity() {
        ScheduleDay targetDay = scheduleDay(TARGET_DAY_ID, TARGET_SCHEDULE_ID, TARGET_DATE);
        ScheduleDay sourceDay = scheduleDay(51L, TARGET_SCHEDULE_ID, LocalDate.of(2026, 10, 26));
        Proposal source = proposal(90L, 11, 11);
        source.setAlternativeGroup("weekly-choice");
        source.setPairParticipant(participant(7L));
        stubAuthorizedTarget(targetDay, participantWithPermissions(true, true));
        stubCurrentScheduleSourceDay(sourceDay);
        stubSelectedSource(sourceDay, List.of(source), 0, List.of((short) 11));
        given(participantRepository.countAssignedParticipantForAgentAndSchedule(AGENT_ID, 7L, TARGET_SCHEDULE_ID))
                .willReturn(1L);

        ProposalPrefillDto result = prefill();

        assertThat(result.getValues())
                .extracting(values -> values.getAlternativeGroup(), values -> values.getPairParticipantId())
                .containsExactly("weekly-choice", 7L);
        verify(participantRepository).countAssignedParticipantForAgentAndSchedule(AGENT_ID, 7L, TARGET_SCHEDULE_ID);
    }

    @Test
    void clearsRestrictedFieldsWhenCurrentPermissionsDoNotAllowThem() {
        ScheduleDay targetDay = scheduleDay(TARGET_DAY_ID, TARGET_SCHEDULE_ID, TARGET_DATE);
        ScheduleDay sourceDay = scheduleDay(52L, TARGET_SCHEDULE_ID, LocalDate.of(2026, 10, 26));
        Proposal source = proposal(91L, 11, 11);
        source.setAlternativeGroup("weekly-choice");
        source.setPairParticipant(participant(7L));
        stubAuthorizedTarget(targetDay, participantWithPermissions(false, false));
        stubCurrentScheduleSourceDay(sourceDay);
        stubSelectedSource(sourceDay, List.of(source), 0, List.of((short) 11));

        ProposalPrefillDto result = prefill();

        assertThat(result.getValues())
                .extracting(values -> values.getAlternativeGroup(), values -> values.getPairParticipantId())
                .containsExactly(null, null);
        verify(participantRepository, never())
                .countAssignedParticipantForAgentAndSchedule(AGENT_ID, 7L, TARGET_SCHEDULE_ID);
    }

    @Test
    void clearsPairWhenItCannotBeSelectedInTheTargetSchedule() {
        ScheduleDay targetDay = scheduleDay(TARGET_DAY_ID, TARGET_SCHEDULE_ID, TARGET_DATE);
        ScheduleDay sourceDay = scheduleDay(53L, TARGET_SCHEDULE_ID, LocalDate.of(2026, 10, 26));
        Proposal source = proposal(92L, 11, 11);
        source.setPairParticipant(participant(7L));
        stubAuthorizedTarget(targetDay, participantWithPermissions(false, true));
        stubCurrentScheduleSourceDay(sourceDay);
        stubSelectedSource(sourceDay, List.of(source), 0, List.of((short) 11));
        given(participantRepository.countAssignedParticipantForAgentAndSchedule(AGENT_ID, 7L, TARGET_SCHEDULE_ID))
                .willReturn(0L);

        ProposalPrefillDto result = prefill();

        assertThat(result.getValues().getPairParticipantId()).isNull();
    }

    @Test
    void clearsPairWhenTheHistoricalPairIsTheTargetParticipant() {
        ScheduleDay targetDay = scheduleDay(TARGET_DAY_ID, TARGET_SCHEDULE_ID, TARGET_DATE);
        ScheduleDay sourceDay = scheduleDay(54L, TARGET_SCHEDULE_ID, LocalDate.of(2026, 10, 26));
        Proposal source = proposal(95L, 11, 11);
        source.setPairParticipant(participant(PARTICIPANT_ID));
        stubAuthorizedTarget(targetDay, participantWithPermissions(false, true));
        stubCurrentScheduleSourceDay(sourceDay);
        stubSelectedSource(sourceDay, List.of(source), 0, List.of((short) 11));

        ProposalPrefillDto result = prefill();

        assertThat(result.getValues().getPairParticipantId()).isNull();
        verify(participantRepository, never())
                .countAssignedParticipantForAgentAndSchedule(AGENT_ID, PARTICIPANT_ID, TARGET_SCHEDULE_ID);
    }

    @Test
    void returnsUnavailableInsteadOfChangingHistoricalValuesThatDoNotFitTheTargetDay() {
        ScheduleDay targetDay = scheduleDay(TARGET_DAY_ID, TARGET_SCHEDULE_ID, TARGET_DATE);
        ScheduleDay sourceDay = scheduleDay(61L, TARGET_SCHEDULE_ID, LocalDate.of(2026, 10, 26));
        Proposal source = proposal(93L, 7, 7);
        stubAuthorizedTarget(targetDay, participantWithPermissions(false, false));
        stubCurrentScheduleSourceDay(sourceDay);
        stubSelectedSource(sourceDay, List.of(source), 0, List.of((short) 9));

        ProposalPrefillDto result = prefill();

        assertThat(result)
                .extracting(ProposalPrefillDto::isAvailable, ProposalPrefillDto::getSource, ProposalPrefillDto::getValues)
                .containsExactly(false, null, null);
        verify(proposalRepository, never()).findPrefillSourceDaysInEarlierSchedules(any(), any(), any(), any());
    }

    @Test
    void doesNotSwitchToAnotherSourceProposalWhenTheSelectedIndexIsIncompatible() {
        ScheduleDay targetDay = scheduleDay(TARGET_DAY_ID, TARGET_SCHEDULE_ID, TARGET_DATE);
        ScheduleDay sourceDay = scheduleDay(63L, TARGET_SCHEDULE_ID, LocalDate.of(2026, 10, 26));
        Proposal validFirst = proposal(96L, 7, 7);
        Proposal incompatibleSecond = proposal(97L, 11, 11);
        stubAuthorizedTarget(targetDay, participantWithPermissions(false, false));
        stubCurrentScheduleSourceDay(sourceDay);
        stubSelectedSource(
                sourceDay,
                List.of(validFirst, incompatibleSecond),
                1,
                List.of((short) 7)
        );

        ProposalPrefillDto result = prefill();

        assertThat(result)
                .extracting(ProposalPrefillDto::isAvailable, ProposalPrefillDto::getSource, ProposalPrefillDto::getValues)
                .containsExactly(false, null, null);
        verify(proposalRepository, never()).findPrefillSourceDaysInEarlierSchedules(any(), any(), any(), any());
    }

    @Test
    void returnsNullBreakLengthsForNoBreakSourceProposal() {
        ScheduleDay targetDay = scheduleDay(TARGET_DAY_ID, TARGET_SCHEDULE_ID, TARGET_DATE);
        ScheduleDay sourceDay = scheduleDay(62L, TARGET_SCHEDULE_ID, LocalDate.of(2026, 10, 26));
        Proposal source = proposal(94L, 11, 11);
        source.setBreakMode(BreakMode.NO_BREAK);
        source.setBreakLengthStart((short) 1);
        source.setBreakLengthEnd((short) 1);
        stubAuthorizedTarget(targetDay, participantWithPermissions(false, false));
        stubCurrentScheduleSourceDay(sourceDay);
        stubSelectedSource(sourceDay, List.of(source), 0, List.of((short) 11));

        ProposalPrefillDto result = prefill();

        assertThat(result.getValues())
                .extracting(values -> values.getBreakMode(), values -> values.getBreakLengthStart(), values -> values.getBreakLengthEnd())
                .containsExactly(0, null, null);
    }

    @Test
    void forbidsPrefillForParticipantOutsideTheCurrentAgentsScope() {
        ScheduleDay targetDay = scheduleDay(TARGET_DAY_ID, TARGET_SCHEDULE_ID, TARGET_DATE);
        given(scheduleDayRepository.findByIdForSchedule(TARGET_DAY_ID, TARGET_SCHEDULE_ID)).willReturn(targetDay);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(participantService)
                .assertAgentCanManageParticipantForSchedule(AGENT_ID, PARTICIPANT_ID, TARGET_SCHEDULE_ID);

        assertThatThrownBy(this::prefill)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verifyNoInteractions(proposalRepository, participantRepository, scheduleEntryRepository);
    }

    private ProposalPrefillDto prefillWithSourceProposalCount(long targetProposalCount) {
        ScheduleDay targetDay = scheduleDay(TARGET_DAY_ID, TARGET_SCHEDULE_ID, TARGET_DATE);
        ScheduleDay sourceDay = scheduleDay(40L, TARGET_SCHEDULE_ID, LocalDate.of(2026, 10, 26));
        Proposal second = proposal(62L, 15, 15);
        Proposal first = proposal(61L, 11, 11);
        stubAuthorizedTarget(targetDay, participantWithPermissions(false, false));
        stubCurrentScheduleSourceDay(sourceDay);
        stubSelectedSource(
                sourceDay,
                List.of(second, first),
                targetProposalCount,
                List.of((short) 11, (short) 15)
        );
        return prefill();
    }

    private void stubAuthorizedTarget(ScheduleDay targetDay, Participant insertingParticipant) {
        given(scheduleDayRepository.findByIdForSchedule(TARGET_DAY_ID, TARGET_SCHEDULE_ID)).willReturn(targetDay);
        given(participantRepository.findById(AGENT_ID)).willReturn(Optional.of(insertingParticipant));
    }

    private void stubCurrentScheduleSourceDay(ScheduleDay sourceDay) {
        given(proposalRepository.findPrefillSourceDaysInSchedule(
                PARTICIPANT_ID,
                AGENT_ID,
                TARGET_DATE,
                TARGET_SCHEDULE_ID
        )).willReturn(List.of(sourceDay));
    }

    private void stubSelectedSource(
            ScheduleDay sourceDay,
            List<Proposal> sourceProposals,
            long targetProposalCount,
            List<Short> targetHours
    ) {
        given(proposalRepository.findForParticipantAndScheduleDayForPrefill(PARTICIPANT_ID, sourceDay.getId()))
                .willReturn(sourceProposals);
        given(proposalRepository.countByParticipant_IdAndScheduleDay_Id(PARTICIPANT_ID, TARGET_DAY_ID))
                .willReturn(targetProposalCount);
        given(scheduleEntryRepository.findHoursForScheduleDay(TARGET_DAY_ID)).willReturn(targetHours);
    }

    private ProposalPrefillDto prefill() {
        return proposalPrefillService.preparePrefill(
                PARTICIPANT_ID,
                TARGET_DAY_ID,
                AGENT_ID,
                TARGET_SCHEDULE_ID
        );
    }

    private Participant participantWithPermissions(boolean allowAlternativeGroupSelection, boolean allowPairSelection) {
        Participant participant = participant(AGENT_ID);
        participant.setAllowAlternativeGroupSelection(allowAlternativeGroupSelection);
        participant.setAllowPairSelection(allowPairSelection);
        return participant;
    }

    private Participant participant(long id) {
        Participant participant = new Participant();
        participant.setId(id);
        return participant;
    }

    private Proposal proposal(long id, int hourStart, int hourEnd) {
        Proposal proposal = new Proposal();
        proposal.setId(id);
        proposal.setHourStart((short) hourStart);
        proposal.setHourEnd((short) hourEnd);
        proposal.setServiceLengthStart((short) 1);
        proposal.setServiceLengthEnd((short) 1);
        proposal.setBreakMode(BreakMode.NO_BREAK);
        return proposal;
    }

    private ScheduleDay scheduleDay(long id, long scheduleId, LocalDate date) {
        Schedule schedule = new Schedule();
        schedule.setId(scheduleId);
        ScheduleDay scheduleDay = new ScheduleDay();
        scheduleDay.setId(id);
        scheduleDay.setSchedule(schedule);
        scheduleDay.setScheduleDate(date);
        return scheduleDay;
    }
}
