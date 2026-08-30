package org.cartscheduler.service;

import org.cartscheduler.dto.rest.request.CreateProposalRequest;
import org.cartscheduler.dto.rest.response.ProposalDto;
import org.cartscheduler.entity.Proposal;
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

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.cartscheduler.support.TestEntityFactory.participant;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private ScheduleDayRepository scheduleDayRepository;

    @Mock
    private ScheduleEntryRepository scheduleEntryRepository;

    @InjectMocks
    private ProposalService proposalService;

    @Test
    void shouldMapProposalsAssignedToParticipantForRequestedScheduleDay() {
        Proposal proposal = proposal(10L, 1L, 2L, 3L, 5L);
        given(scheduleDayRepository.findByIdForSchedule(5L, 20L)).willReturn(scheduleDay(5L));
        given(participantRepository.countAssignedParticipantForAgentAndSchedule(3L, 1L, 20L)).willReturn(1L);
        given(proposalRepository.findForParticipantAndScheduleDay(1L, 5L)).willReturn(List.of(proposal));

        var result = proposalService.prepareProposalDto(1L, 5L, 3L, 20L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst())
                .extracting(
                        dto -> dto.getId(),
                        dto -> dto.getParticipantId(),
                        dto -> dto.getPairParticipantId(),
                        dto -> dto.getInsertingParticipantId(),
                        dto -> dto.getScheduleDayId(),
                        dto -> dto.getHourStart(),
                        dto -> dto.getHourEnd(),
                        dto -> dto.getBreakLengthStart(),
                        dto -> dto.getBreakLengthEnd(),
                        dto -> dto.getServiceLengthStart(),
                        dto -> dto.getServiceLengthEnd()
                )
                .containsExactly(10L, 1L, 2L, 3L, 5L, 8, 12, (short) 1, (short) 2, (short) 3, (short) 4);
        verify(proposalRepository).findForParticipantAndScheduleDay(1L, 5L);
    }

    @Test
    void shouldReturnEmptyListWhenParticipantHasNoProposalForScheduleDay() {
        given(scheduleDayRepository.findByIdForSchedule(5L, 20L)).willReturn(scheduleDay(5L));
        given(participantRepository.countAssignedParticipantForAgentAndSchedule(3L, 1L, 20L)).willReturn(1L);
        given(proposalRepository.findForParticipantAndScheduleDay(1L, 5L)).willReturn(List.of());

        var result = proposalService.prepareProposalDto(1L, 5L, 3L, 20L);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldCreateProposalWithAuthenticatedParticipantAsInserter() {
        CreateProposalRequest request = validRequest();
        ScheduleDay scheduleDay = scheduleDay(10L);
        Date generatedDate = Date.from(Instant.parse("2026-08-27T10:00:00Z"));
        stubValidCreate(request, scheduleDay);
        given(proposalRepository.saveAndFlush(any(Proposal.class))).willAnswer(invocation -> {
            Proposal savedProposal = invocation.getArgument(0);
            assertThat(savedProposal.getDateAdd()).isNotNull();
            assertThat(savedProposal.getPairParticipant()).isNull();
            savedProposal.setId(99L);
            savedProposal.setDateAdd(generatedDate);
            return savedProposal;
        });

        ProposalDto result = proposalService.createProposal(request, 1L, 5L);

        assertThat(result)
                .extracting(
                        ProposalDto::getId,
                        ProposalDto::getDateAdd,
                        ProposalDto::getParticipantId,
                        ProposalDto::getPairParticipantId,
                        ProposalDto::getInsertingParticipantId,
                        ProposalDto::getScheduleDayId,
                        ProposalDto::getHourStart,
                        ProposalDto::getHourEnd,
                        ProposalDto::getServiceLengthStart,
                        ProposalDto::getServiceLengthEnd,
                        ProposalDto::getBreakLengthStart,
                        ProposalDto::getBreakLengthEnd
                )
                .containsExactly(99L, generatedDate, 2L, null, 1L, 10L, 7, 15,
                        (short) 4, (short) 6, (short) 1, (short) 2);
        verify(proposalRepository).saveAndFlush(any(Proposal.class));
    }

    @Test
    void shouldCreateProposalForSingleScheduleHour() {
        CreateProposalRequest request = new CreateProposalRequest(2L, 10L, 17, 17, 1, 1, null, null);
        ScheduleDay scheduleDay = scheduleDay(10L);
        stubValidCreate(request, scheduleDay, List.of((short) 17));
        given(proposalRepository.saveAndFlush(any(Proposal.class))).willAnswer(invocation -> {
            Proposal savedProposal = invocation.getArgument(0);
            savedProposal.setId(100L);
            savedProposal.setDateAdd(Date.from(Instant.parse("2026-08-30T10:00:00Z")));
            return savedProposal;
        });

        ProposalDto result = proposalService.createProposal(request, 1L, 5L);

        assertThat(result)
                .extracting(
                        ProposalDto::getHourStart,
                        ProposalDto::getHourEnd,
                        ProposalDto::getServiceLengthStart,
                        ProposalDto::getServiceLengthEnd,
                        ProposalDto::getBreakLengthStart,
                        ProposalDto::getBreakLengthEnd
                )
                .containsExactly(17, 17, (short) 1, (short) 1, null, null);
    }

    @Test
    void shouldCreateProposalForEntireInclusiveHourRange() {
        CreateProposalRequest request = new CreateProposalRequest(2L, 10L, 7, 17, 11, 11, null, null);
        ScheduleDay scheduleDay = scheduleDay(10L);
        stubValidCreate(request, scheduleDay, List.of((short) 7, (short) 17));
        stubSavedProposal();

        ProposalDto result = proposalService.createProposal(request, 1L, 5L);

        assertThat(result)
                .extracting(
                        ProposalDto::getHourStart,
                        ProposalDto::getHourEnd,
                        ProposalDto::getServiceLengthStart,
                        ProposalDto::getServiceLengthEnd
                )
                .containsExactly(7, 17, (short) 11, (short) 11);
    }

    @Test
    void shouldCalculateBreakLengthFromInclusiveHourRange() {
        CreateProposalRequest request = new CreateProposalRequest(2L, 10L, 7, 15, 4, 4, 4, 5);
        ScheduleDay scheduleDay = scheduleDay(10L);
        stubValidCreate(request, scheduleDay);
        stubSavedProposal();

        ProposalDto result = proposalService.createProposal(request, 1L, 5L);

        assertThat(result)
                .extracting(ProposalDto::getBreakLengthStart, ProposalDto::getBreakLengthEnd)
                .containsExactly((short) 4, (short) 5);
    }

    @Test
    void shouldForbidProposalWhenAuthenticatedParticipantIsNotAgentOfTargetParticipant() {
        CreateProposalRequest request = validRequest();
        ScheduleDay scheduleDay = scheduleDay(10L);
        given(participantRepository.findById(2L)).willReturn(Optional.of(participant(2L)));
        given(scheduleDayRepository.findByIdForSchedule(10L, 5L)).willReturn(scheduleDay);
        given(participantRepository.countAssignedParticipantForAgentAndSchedule(1L, 2L, 5L)).willReturn(0L);

        assertForbidden(() -> proposalService.createProposal(request, 1L, 5L));

        verifyNoInteractions(scheduleEntryRepository, proposalRepository);
    }

    @Test
    void shouldForbidProposalListingWhenAuthenticatedParticipantIsNotAgentOfTargetParticipant() {
        given(scheduleDayRepository.findByIdForSchedule(10L, 5L)).willReturn(scheduleDay(10L));
        given(participantRepository.countAssignedParticipantForAgentAndSchedule(1L, 2L, 5L)).willReturn(0L);

        assertForbidden(() -> proposalService.prepareProposalDto(2L, 10L, 1L, 5L));

        verifyNoInteractions(proposalRepository);
    }

    @Test
    void shouldForbidProposalForScheduleDayOutsideScheduleFromToken() {
        CreateProposalRequest request = validRequest();
        given(participantRepository.findById(2L)).willReturn(Optional.of(participant(2L)));
        given(scheduleDayRepository.findByIdForSchedule(10L, 5L)).willReturn(null);

        assertThatThrownBy(() -> proposalService.createProposal(request, 1L, 5L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verifyNoInteractions(scheduleEntryRepository, proposalRepository);
    }

    @Test
    void shouldRejectProposalWhenHoursAreNotConfiguredForScheduleDay() {
        CreateProposalRequest request = new CreateProposalRequest(2L, 10L, 8, 15, 4, 6, 1, 2);
        stubValidCreate(request, scheduleDay(10L));

        assertBadRequest(() -> proposalService.createProposal(request, 1L, 5L));

        verifyNoInteractions(proposalRepository);
    }

    @Test
    void shouldRejectProposalWhenHourEndIsBeforeHourStart() {
        CreateProposalRequest request = new CreateProposalRequest(2L, 10L, 15, 7, 4, 6, 1, 2);
        stubValidCreate(request, scheduleDay(10L));

        assertBadRequest(() -> proposalService.createProposal(request, 1L, 5L));

        verifyNoInteractions(proposalRepository);
    }

    @Test
    void shouldRejectProposalWhenServiceLengthRangeIsInvalid() {
        CreateProposalRequest request = new CreateProposalRequest(2L, 10L, 7, 15, 5, 4, 1, 2);
        stubValidCreate(request, scheduleDay(10L));

        assertBadRequest(() -> proposalService.createProposal(request, 1L, 5L));

        verifyNoInteractions(proposalRepository);
    }

    @Test
    void shouldRejectProposalWhenOnlyOneBreakLengthBoundaryIsProvided() {
        CreateProposalRequest request = new CreateProposalRequest(2L, 10L, 7, 15, 4, 6, null, 2);
        stubValidCreate(request, scheduleDay(10L));

        assertBadRequest(() -> proposalService.createProposal(request, 1L, 5L));

        verifyNoInteractions(proposalRepository);
    }

    @Test
    void shouldRejectProposalWhenBreakLengthStartIsNotLessThanRemainingDuration() {
        CreateProposalRequest request = new CreateProposalRequest(2L, 10L, 7, 15, 4, 6, 5, 5);
        stubValidCreate(request, scheduleDay(10L));

        assertBadRequest(() -> proposalService.createProposal(request, 1L, 5L));

        verifyNoInteractions(proposalRepository);
    }

    @Test
    void shouldExplicitlyReportThatDeletingProposalIsNotImplemented() {
        assertThatThrownBy(() -> proposalService.delete(1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_IMPLEMENTED));
    }

    private void stubValidCreate(CreateProposalRequest request, ScheduleDay scheduleDay) {
        stubValidCreate(request, scheduleDay, List.of((short) 7, (short) 15));
    }

    private void stubValidCreate(
            CreateProposalRequest request,
            ScheduleDay scheduleDay,
            List<Short> scheduleHours
    ) {
        given(participantRepository.findById(request.getParticipantId()))
                .willReturn(Optional.of(participant(request.getParticipantId())));
        given(participantRepository.findById(1L)).willReturn(Optional.of(participant(1L)));
        given(scheduleDayRepository.findByIdForSchedule(request.getScheduleDayId(), 5L)).willReturn(scheduleDay);
        given(participantRepository.countAssignedParticipantForAgentAndSchedule(1L, request.getParticipantId(), 5L))
                .willReturn(1L);
        given(scheduleEntryRepository.findHoursForScheduleDay(scheduleDay.getId()))
                .willReturn(scheduleHours);
    }

    private CreateProposalRequest validRequest() {
        return new CreateProposalRequest(2L, 10L, 7, 15, 4, 6, 1, 2);
    }

    private void stubSavedProposal() {
        given(proposalRepository.saveAndFlush(any(Proposal.class))).willAnswer(invocation -> {
            Proposal savedProposal = invocation.getArgument(0);
            savedProposal.setId(100L);
            savedProposal.setDateAdd(Date.from(Instant.parse("2026-08-30T10:00:00Z")));
            return savedProposal;
        });
    }

    private ScheduleDay scheduleDay(long id) {
        ScheduleDay scheduleDay = new ScheduleDay();
        scheduleDay.setId(id);
        return scheduleDay;
    }

    private void assertBadRequest(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    private Proposal proposal(long id, long participantId, long pairParticipantId, long insertingParticipantId, long scheduleDayId) {
        ScheduleDay scheduleDay = new ScheduleDay();
        scheduleDay.setId(scheduleDayId);

        Proposal proposal = new Proposal();
        proposal.setId(id);
        proposal.setDateAdd(Date.from(Instant.parse("2026-08-26T10:00:00Z")));
        proposal.setParticipant(participant(participantId));
        proposal.setPairParticipant(participant(pairParticipantId));
        proposal.setInsertingParticipant(participant(insertingParticipantId));
        proposal.setScheduleDay(scheduleDay);
        proposal.setHourStart((short) 8);
        proposal.setHourEnd((short) 12);
        proposal.setBreakLengthStart((short) 1);
        proposal.setBreakLengthEnd((short) 2);
        proposal.setServiceLengthStart((short) 3);
        proposal.setServiceLengthEnd((short) 4);
        return proposal;
    }
}
