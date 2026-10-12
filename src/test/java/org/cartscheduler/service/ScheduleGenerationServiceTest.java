package org.cartscheduler.service;

import java.util.ArrayList;
import java.util.List;
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
        given(proposalRepository.findForSchedule(10L)).willReturn(List.of(
                proposal(configuredParticipant, day, (short) 8, (short) 11, (short) 1, (short) 1, (short) 1, (short) 1),
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
}
