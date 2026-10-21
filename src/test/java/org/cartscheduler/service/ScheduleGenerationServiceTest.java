package org.cartscheduler.service;

import java.util.ArrayList;
import java.util.List;
import org.cartscheduler.entity.BreakMode;
import org.cartscheduler.entity.ConfigParticipantBreakLength;
import org.cartscheduler.entity.ConfigParticipantServiceLength;
import org.cartscheduler.entity.ConfigRowFullOrEmptyScore;
import org.cartscheduler.entity.EnforcedParticipant;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.Proposal;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.entity.ScheduleEntry;
import org.cartscheduler.repository.ConfigCandidateUniquenessScoreRepository;
import org.cartscheduler.repository.ConfigPairDiversityScoreRepository;
import org.cartscheduler.repository.ConfigPairRuleRepository;
import org.cartscheduler.repository.ConfigPairScoreRepository;
import org.cartscheduler.repository.ConfigParticipantBreakLengthRepository;
import org.cartscheduler.repository.ConfigParticipantPrivilegeScoreRepository;
import org.cartscheduler.repository.ConfigParticipantServiceLengthRepository;
import org.cartscheduler.repository.ConfigRowFullOrEmptyScoreRepository;
import org.cartscheduler.repository.EnforcedParticipantRepository;
import org.cartscheduler.repository.ProposalRepository;
import org.cartscheduler.repository.ScheduleDayRepository;
import org.cartscheduler.repository.ScheduleEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.cartscheduler.support.TestEntityFactory.participant;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ScheduleGenerationServiceTest {
    @Mock
    private ScheduleDayRepository scheduleDayRepository;

    @Mock
    private ScheduleEntryRepository scheduleEntryRepository;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private EnforcedParticipantRepository enforcedParticipantRepository;

    @Mock
    private ConfigRowFullOrEmptyScoreRepository rowFullOrEmptyScoreRepository;

    @Mock
    private ConfigPairScoreRepository pairScoreRepository;

    @Mock
    private ConfigCandidateUniquenessScoreRepository candidateUniquenessScoreRepository;

    @Mock
    private ConfigParticipantPrivilegeScoreRepository participantPrivilegeScoreRepository;

    @Mock
    private ConfigPairDiversityScoreRepository pairDiversityScoreRepository;

    @Mock
    private ConfigPairRuleRepository pairRuleRepository;

    @Mock
    private ConfigParticipantBreakLengthRepository participantBreakLengthRepository;

    @Mock
    private ConfigParticipantServiceLengthRepository participantServiceLengthRepository;

    @InjectMocks
    private ScheduleGenerationService scheduleGenerationService;

    @Test
    void requiresTheMandatoryRowScoreConfiguration() {
        given(rowFullOrEmptyScoreRepository.findAll()).willReturn(List.of());

        assertThatThrownBy(() -> scheduleGenerationService.generate(10L))
                .isInstanceOf(ScheduleGenerationException.class)
                .hasMessageContaining("config_row_full_or_empty_scores");

        verifyNoInteractions(scheduleDayRepository, proposalRepository, scheduleEntryRepository);
    }

    @Test
    void mapsProposalsToGeneratorCellsAndPersistsTheGeneratedParticipants() {
        ScheduleDay day = day(100L, "Monday", (short) 8);
        Participant first = participant(1L);
        Participant second = participant(2L);
        stubRequiredConfiguration();
        stubOptionalEmptyConfigurations();
        given(scheduleDayRepository.findForSchedule(10L)).willReturn(List.of(day));
        given(proposalRepository.findForSchedule(10L)).willReturn(List.of(
                proposal(first, day, (short) 8, (short) 8),
                proposal(second, day, (short) 8, (short) 8)
        ));
        given(enforcedParticipantRepository.findByDayNameIn(any())).willReturn(List.of());

        ScheduleGenerationResult result = scheduleGenerationService.generate(10L);

        ScheduleEntry entry = day.getScheduleEntries().getFirst();
        assertThat(result).isEqualTo(new ScheduleGenerationResult(10L, 2));
        assertThat(entry.getParticipant1()).isSameAs(first);
        assertThat(entry.getParticipant2()).isSameAs(second);
        verify(scheduleEntryRepository).saveAll(List.of(entry));
    }

    @Test
    void leavesARowEmptyWhenOnlyOneParticipantQualifiesForIt() {
        ScheduleDay day = day(100L, "Monday", (short) 8);
        Participant onlyParticipant = participant(1L);
        stubRequiredConfiguration();
        stubOptionalEmptyConfigurations();
        given(scheduleDayRepository.findForSchedule(10L)).willReturn(List.of(day));
        given(proposalRepository.findForSchedule(10L)).willReturn(List.of(
                proposal(onlyParticipant, day, (short) 8, (short) 8)
        ));
        given(enforcedParticipantRepository.findByDayNameIn(any())).willReturn(List.of());

        ScheduleGenerationResult result = scheduleGenerationService.generate(10L);

        ScheduleEntry entry = day.getScheduleEntries().getFirst();
        assertThat(result).isEqualTo(new ScheduleGenerationResult(10L, 0));
        assertThat(entry.getParticipant1()).isNull();
        assertThat(entry.getParticipant2()).isNull();
    }

    @Test
    void previewsTheGeneratedScheduleWithoutPersistingParticipants() {
        ScheduleDay day = day(100L, "Monday", (short) 8);
        Participant first = participant(1L);
        Participant second = participant(2L);
        stubRequiredConfiguration();
        stubOptionalEmptyConfigurations();
        given(scheduleDayRepository.findForSchedule(10L)).willReturn(List.of(day));
        given(proposalRepository.findForSchedule(10L)).willReturn(List.of(
                proposal(first, day, (short) 8, (short) 8),
                proposal(second, day, (short) 8, (short) 8)
        ));
        given(enforcedParticipantRepository.findByDayNameIn(any())).willReturn(List.of());

        ScheduleGenerationPreview preview = scheduleGenerationService.preview(10L);

        assertThat(preview).isEqualTo(new ScheduleGenerationPreview(10L, 2, 10, false));
        assertThat(day.getScheduleEntries().getFirst().getParticipant1()).isNull();
        assertThat(day.getScheduleEntries().getFirst().getParticipant2()).isNull();
        verifyNoInteractions(scheduleEntryRepository);
    }

    @Test
    void honorsThePairParticipantRequestedInAProposal() {
        ScheduleDay day = day(100L, "Monday", (short) 8);
        Participant first = participant(1L);
        Participant requestedPairParticipant = participant(2L);
        Participant otherParticipant = participant(3L);
        Proposal proposalWithPair = proposal(first, day, (short) 8, (short) 8, (short) 1, (short) 1, null, null);
        proposalWithPair.setPairParticipant(requestedPairParticipant);
        stubRequiredConfiguration();
        stubOptionalEmptyConfigurations();
        given(scheduleDayRepository.findForSchedule(10L)).willReturn(List.of(day));
        given(proposalRepository.findForSchedule(10L)).willReturn(List.of(
                proposalWithPair,
                proposal(requestedPairParticipant, day, (short) 8, (short) 8),
                proposal(otherParticipant, day, (short) 8, (short) 8)
        ));
        given(enforcedParticipantRepository.findByDayNameIn(any())).willReturn(List.of());

        scheduleGenerationService.generate(10L);

        ScheduleEntry entry = day.getScheduleEntries().getFirst();
        assertThat(List.of(entry.getParticipant1().getId(), entry.getParticipant2().getId()))
                .containsExactlyInAnyOrder(first.getId(), requestedPairParticipant.getId());
    }

    @Test
    void ignoresProposalWhenRequestedPairParticipantHasNoCompatibleProposal() {
        ScheduleDay day = day(100L, "Monday", (short) 8, (short) 9);
        Participant first = participant(1L);
        Participant unavailablePartner = participant(2L);
        Proposal proposalWithUnavailablePartner = proposal(
                first,
                day,
                (short) 8,
                (short) 9,
                (short) 1,
                (short) 1,
                null,
                null
        );
        proposalWithUnavailablePartner.setPairParticipant(unavailablePartner);
        stubRequiredConfiguration();
        stubOptionalEmptyConfigurations();
        given(scheduleDayRepository.findForSchedule(10L)).willReturn(List.of(day));
        given(proposalRepository.findForSchedule(10L)).willReturn(List.of(proposalWithUnavailablePartner));
        given(enforcedParticipantRepository.findByDayNameIn(any())).willReturn(List.of());

        ScheduleGenerationResult result = scheduleGenerationService.generate(10L);

        assertThat(result).isEqualTo(new ScheduleGenerationResult(10L, 0));
        assertThat(day.getScheduleEntries())
                .allSatisfy(entry -> {
                    assertThat(entry.getParticipant1()).isNull();
                    assertThat(entry.getParticipant2()).isNull();
                });
        verify(scheduleEntryRepository).saveAll(day.getScheduleEntries());
    }

    @Test
    void omitsAnOptionalRequestedPairWhoseMinimumServiceCannotBeFulfilled() {
        ScheduleDay day = day(100L, "Monday", (short) 8, (short) 9);
        Participant requester = participant(1L);
        Participant requestedPartner = participant(2L);
        Participant firstFallback = participant(3L);
        Participant secondFallback = participant(4L);
        Proposal requesterProposal = proposal(
                requester,
                day,
                (short) 8,
                (short) 9,
                (short) 2,
                (short) 2,
                null,
                null
        );
        requesterProposal.setPairParticipant(requestedPartner);

        stubRequiredConfiguration();
        stubOptionalEmptyConfigurations();
        given(scheduleDayRepository.findForSchedule(10L)).willReturn(List.of(day));
        given(proposalRepository.findForSchedule(10L)).willReturn(List.of(
                requesterProposal,
                proposal(requestedPartner, day, (short) 8, (short) 8),
                proposal(firstFallback, day, (short) 8, (short) 9),
                proposal(secondFallback, day, (short) 8, (short) 9)
        ));
        given(enforcedParticipantRepository.findByDayNameIn(any())).willReturn(List.of());

        ScheduleGenerationResult result = scheduleGenerationService.generate(10L);

        assertThat(result).isEqualTo(new ScheduleGenerationResult(10L, 4));
        assertThat(day.getScheduleEntries())
                .allSatisfy(entry -> {
                    assertThat(isParticipantAssigned(entry, requester.getId())).isFalse();
                    assertThat(isParticipantAssigned(entry, requestedPartner.getId())).isFalse();
                });
    }

    @Test
    void choosesOneProposalFromAnAlternativeGroupInsteadOfMergingConflictingServiceLengths() {
        ScheduleDay day = day(100L, "Monday", (short) 12, (short) 13);
        Participant alternativeParticipant = participant(18L);
        Participant otherParticipant = participant(2L);
        Proposal oneHourOption = proposal(
                alternativeParticipant,
                day,
                (short) 12,
                (short) 12,
                (short) 1,
                (short) 1,
                null,
                null
        );
        oneHourOption.setId(1001L);
        oneHourOption.setAlternativeGroup("hours");
        Proposal twoHourOption = proposal(
                alternativeParticipant,
                day,
                (short) 12,
                (short) 13,
                (short) 2,
                (short) 2,
                null,
                null
        );
        twoHourOption.setId(1002L);
        twoHourOption.setAlternativeGroup("hours");

        stubRequiredConfiguration();
        stubOptionalEmptyConfigurations();
        given(scheduleDayRepository.findForSchedule(10L)).willReturn(List.of(day));
        given(proposalRepository.findForSchedule(10L)).willReturn(List.of(
                oneHourOption,
                twoHourOption,
                proposal(otherParticipant, day, (short) 12, (short) 13)
        ));
        given(enforcedParticipantRepository.findByDayNameIn(any())).willReturn(List.of());

        scheduleGenerationService.generate(10L);

        assertThat(assignedHours(day, alternativeParticipant.getId()))
                .containsExactly((short) 12, (short) 13);
    }

    @Test
    void choosesAtMostOneAlternativeProposalAcrossAllScheduleDays() {
        ScheduleDay monday = day(100L, "Monday", (short) 8);
        ScheduleDay wednesday = day(101L, "Wednesday", (short) 8);
        wednesday.setDayOfWeek((short) 3);
        Participant alternativeParticipant = participant(18L);
        Participant otherParticipant = participant(2L);
        Proposal mondayOption = proposal(
                alternativeParticipant,
                monday,
                (short) 8,
                (short) 8,
                (short) 1,
                (short) 1,
                null,
                null
        );
        mondayOption.setId(1001L);
        mondayOption.setAlternativeGroup("once-a-week");
        Proposal wednesdayOption = proposal(
                alternativeParticipant,
                wednesday,
                (short) 8,
                (short) 8,
                (short) 1,
                (short) 1,
                null,
                null
        );
        wednesdayOption.setId(1002L);
        wednesdayOption.setAlternativeGroup("once-a-week");

        stubRequiredConfiguration();
        stubOptionalEmptyConfigurations();
        given(scheduleDayRepository.findForSchedule(10L)).willReturn(List.of(monday, wednesday));
        given(proposalRepository.findForSchedule(10L)).willReturn(List.of(
                mondayOption,
                wednesdayOption,
                proposal(otherParticipant, monday, (short) 8, (short) 8),
                proposal(otherParticipant, wednesday, (short) 8, (short) 8)
        ));
        given(enforcedParticipantRepository.findByDayNameIn(any())).willReturn(List.of());

        scheduleGenerationService.generate(10L);

        long alternativeAssignments = List.of(monday, wednesday).stream()
                .flatMap(day -> day.getScheduleEntries().stream())
                .filter(entry -> isParticipantAssigned(entry, alternativeParticipant.getId()))
                .count();
        assertThat(alternativeAssignments).isEqualTo(1);
    }

    @Test
    void fillsEveryEntryOfAMatchingDayWithEnforcedParticipants() {
        ScheduleDay day = day(100L, "Monday", (short) 8, (short) 9);
        Participant first = participant(11L);
        Participant second = participant(12L);
        EnforcedParticipant enforced = new EnforcedParticipant();
        enforced.setDayName("Monday");
        enforced.setParticipant1(first);
        enforced.setParticipant2(second);
        stubRequiredConfiguration();
        stubOptionalEmptyConfigurations();
        given(scheduleDayRepository.findForSchedule(10L)).willReturn(List.of(day));
        given(proposalRepository.findForSchedule(10L)).willReturn(List.of());
        given(enforcedParticipantRepository.findByDayNameIn(any())).willReturn(List.of(enforced));

        ScheduleGenerationResult result = scheduleGenerationService.generate(10L);

        assertThat(result.assignedParticipantCells()).isEqualTo(4);
        assertThat(day.getScheduleEntries())
                .allSatisfy(entry -> {
                    assertThat(entry.getParticipant1()).isSameAs(first);
                    assertThat(entry.getParticipant2()).isSameAs(second);
                });
        verify(scheduleEntryRepository).saveAll(day.getScheduleEntries());
    }

    @Test
    void givesDatabaseServiceAndBreakLengthsPriorityOverProposalLengths() {
        ScheduleDay day = day(100L, "Monday", (short) 8, (short) 9, (short) 10, (short) 11);
        Participant configuredParticipant = participant(1L);
        Participant otherParticipant = participant(2L);
        ConfigParticipantServiceLength serviceLength = new ConfigParticipantServiceLength();
        serviceLength.setParticipant(configuredParticipant);
        serviceLength.setServiceLengthStart((short) 2);
        serviceLength.setServiceLengthEnd((short) 2);
        ConfigParticipantBreakLength breakLength = new ConfigParticipantBreakLength();
        breakLength.setParticipant(configuredParticipant);
        breakLength.setBreakLengthStart((short) 2);
        breakLength.setBreakLengthEnd((short) 2);
        stubRequiredConfiguration();
        stubOptionalEmptyConfigurations();
        given(participantServiceLengthRepository.findAll()).willReturn(List.of(serviceLength));
        given(participantBreakLengthRepository.findAll()).willReturn(List.of(breakLength));
        given(scheduleDayRepository.findForSchedule(10L)).willReturn(List.of(day));
        Proposal configuredProposal = proposal(
                configuredParticipant,
                day,
                (short) 8,
                (short) 11,
                (short) 1,
                (short) 1,
                (short) 1,
                (short) 1
        );
        configuredProposal.setBreakMode(BreakMode.REQUIRE_BREAK);
        given(proposalRepository.findForSchedule(10L)).willReturn(List.of(
                configuredProposal,
                proposal(otherParticipant, day, (short) 8, (short) 11)
        ));
        given(enforcedParticipantRepository.findByDayNameIn(any())).willReturn(List.of());

        scheduleGenerationService.generate(10L);

        assertThat(day.getScheduleEntries().stream()
                .filter(entry -> isParticipantAssigned(entry, configuredParticipant.getId()))
                .map(ScheduleEntry::getHour)
                .toList())
                .containsExactly((short) 8, (short) 11);
    }

    @Test
    void keepsAnAssignedNoBreakParticipantInOneContinuousBlock() {
        ScheduleDay day = day(100L, "Monday", (short) 8, (short) 9, (short) 10);
        Participant noBreakParticipant = participant(1L);
        Participant otherParticipant = participant(2L);
        Proposal noBreakProposal = proposal(
                noBreakParticipant,
                day,
                (short) 8,
                (short) 10,
                (short) 2,
                (short) 2,
                null,
                null
        );
        noBreakProposal.setBreakMode(BreakMode.NO_BREAK);
        stubRequiredConfiguration();
        stubOptionalEmptyConfigurations();
        given(scheduleDayRepository.findForSchedule(10L)).willReturn(List.of(day));
        given(proposalRepository.findForSchedule(10L)).willReturn(List.of(
                noBreakProposal,
                proposal(otherParticipant, day, (short) 8, (short) 10)
        ));
        given(enforcedParticipantRepository.findByDayNameIn(any())).willReturn(List.of());

        scheduleGenerationService.generate(10L);

        List<Short> assignedHours = assignedHours(day, noBreakParticipant.getId());
        assertThat(assignedHours).hasSize(2);
        assertThat(assignedHours.get(1).intValue() - assignedHours.getFirst().intValue()).isEqualTo(1);
    }

    @Test
    void requiresABreakWithTheRequestedLength() {
        ScheduleDay day = day(100L, "Monday", (short) 8, (short) 9, (short) 10, (short) 11);
        Participant breakParticipant = participant(1L);
        Participant otherParticipant = participant(2L);
        Proposal breakProposal = proposal(
                breakParticipant,
                day,
                (short) 8,
                (short) 11,
                (short) 2,
                (short) 2,
                (short) 1,
                (short) 1
        );
        breakProposal.setBreakMode(BreakMode.REQUIRE_BREAK);
        stubRequiredConfiguration();
        stubOptionalEmptyConfigurations();
        given(scheduleDayRepository.findForSchedule(10L)).willReturn(List.of(day));
        given(proposalRepository.findForSchedule(10L)).willReturn(List.of(
                breakProposal,
                proposal(otherParticipant, day, (short) 8, (short) 11)
        ));
        given(enforcedParticipantRepository.findByDayNameIn(any())).willReturn(List.of());

        scheduleGenerationService.generate(10L);

        List<Short> assignedHours = assignedHours(day, breakParticipant.getId());
        assertThat(assignedHours).hasSize(2);
        assertThat(assignedHours.get(1).intValue() - assignedHours.getFirst().intValue()).isEqualTo(2);
    }

    private void stubRequiredConfiguration() {
        ConfigRowFullOrEmptyScore configuration = new ConfigRowFullOrEmptyScore();
        configuration.setEmptyRowPoints(0);
        configuration.setFullRowAdjacentToIncompletePoints(10);
        configuration.setFullRowNotAdjacentToIncompletePoints(10);
        given(rowFullOrEmptyScoreRepository.findAll()).willReturn(List.of(configuration));
    }

    private void stubOptionalEmptyConfigurations() {
        given(pairScoreRepository.findAll()).willReturn(List.of());
        given(candidateUniquenessScoreRepository.findAll()).willReturn(List.of());
        given(participantPrivilegeScoreRepository.findAll()).willReturn(List.of());
        given(pairDiversityScoreRepository.findAll()).willReturn(List.of());
        given(pairRuleRepository.findAll()).willReturn(List.of());
        given(participantBreakLengthRepository.findAll()).willReturn(List.of());
        given(participantServiceLengthRepository.findAll()).willReturn(List.of());
    }

    private ScheduleDay day(long id, String name, short... hours) {
        ScheduleDay day = new ScheduleDay();
        day.setId(id);
        day.setName(name);
        day.setDayOfWeek((short) 1);
        List<ScheduleEntry> entries = new ArrayList<>();
        for (int index = 0; index < hours.length; index++) {
            ScheduleEntry entry = new ScheduleEntry();
            entry.setId(index + 1L);
            entry.setScheduleDay(day);
            entry.setName(hours[index] + ":00");
            entry.setHour(hours[index]);
            entries.add(entry);
        }
        day.setScheduleEntries(entries);
        return day;
    }

    private Proposal proposal(Participant participant, ScheduleDay day, short hourStart, short hourEnd) {
        Proposal proposal = proposal(participant, day, hourStart, hourEnd, null, null, null, null);
        return proposal;
    }

    private Proposal proposal(
            Participant participant,
            ScheduleDay day,
            short hourStart,
            short hourEnd,
            Short serviceLengthStart,
            Short serviceLengthEnd,
            Short breakLengthStart,
            Short breakLengthEnd
    ) {
        Proposal proposal = new Proposal();
        proposal.setParticipant(participant);
        proposal.setScheduleDay(day);
        proposal.setHourStart(hourStart);
        proposal.setHourEnd(hourEnd);
        proposal.setServiceLengthStart(serviceLengthStart);
        proposal.setServiceLengthEnd(serviceLengthEnd);
        proposal.setBreakLengthStart(breakLengthStart);
        proposal.setBreakLengthEnd(breakLengthEnd);
        return proposal;
    }

    private boolean isParticipantAssigned(ScheduleEntry entry, long participantId) {
        return (entry.getParticipant1() != null && entry.getParticipant1().getId() == participantId)
                || (entry.getParticipant2() != null && entry.getParticipant2().getId() == participantId);
    }

    private List<Short> assignedHours(ScheduleDay day, long participantId) {
        return day.getScheduleEntries().stream()
                .filter(entry -> isParticipantAssigned(entry, participantId))
                .map(ScheduleEntry::getHour)
                .toList();
    }
}
