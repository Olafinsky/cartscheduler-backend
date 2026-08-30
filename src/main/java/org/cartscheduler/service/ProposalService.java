package org.cartscheduler.service;

import org.cartscheduler.dto.rest.request.CreateProposalRequest;
import org.cartscheduler.dto.rest.response.ProposalDto;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.Proposal;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.repository.ProposalRepository;
import org.cartscheduler.repository.ScheduleDayRepository;
import org.cartscheduler.repository.ScheduleEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProposalService {

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ScheduleDayRepository scheduleDayRepository;

    @Autowired
    private ScheduleEntryRepository scheduleEntryRepository;

    @Transactional(readOnly = true)
    public List<ProposalDto> prepareProposalDto(
            Long participantId,
            Long scheduleDayId,
            Long agentParticipantId,
            Long scheduleIdFromToken
    ) {
        findScheduleDayForToken(scheduleDayId, scheduleIdFromToken);
        assertAgentCanManageParticipant(agentParticipantId, participantId, scheduleIdFromToken);

        return proposalRepository.findForParticipantAndScheduleDay(participantId, scheduleDayId).stream()
                .map(ProposalDto::new)
                .toList();
    }

    @Transactional
    public ProposalDto createProposal(
            CreateProposalRequest request,
            Long insertingParticipantId,
            Long scheduleIdFromToken
    ) {
        Participant participant = participantRepository.findById(request.getParticipantId())
                .orElseThrow(() -> badRequest("Participant does not exist"));
        ScheduleDay scheduleDay = findScheduleDayForToken(request.getScheduleDayId(), scheduleIdFromToken);
        assertAgentCanManageParticipant(insertingParticipantId, participant.getId(), scheduleIdFromToken);
        Participant insertingParticipant = participantRepository.findById(insertingParticipantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated participant does not exist"
                ));

        validateProposalValues(request, scheduleDay.getId());

        Proposal proposal = new Proposal();
        proposal.setDateAdd(new Date());
        proposal.setParticipant(participant);
        proposal.setInsertingParticipant(insertingParticipant);
        proposal.setScheduleDay(scheduleDay);
        proposal.setHourStart(request.getHourStart().shortValue());
        proposal.setHourEnd(request.getHourEnd().shortValue());
        proposal.setServiceLengthStart(request.getServiceLengthStart().shortValue());
        proposal.setServiceLengthEnd(request.getServiceLengthEnd().shortValue());
        proposal.setBreakLengthStart(toShort(request.getBreakLengthStart()));
        proposal.setBreakLengthEnd(toShort(request.getBreakLengthEnd()));

        return new ProposalDto(proposalRepository.saveAndFlush(proposal));
    }

    @Transactional
    public void delete(Long id, Long deletingParticipantId, Long scheduleIdFromToken) {
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> notFound("Proposal does not exist"));
        findScheduleDayForToken(proposal.getScheduleDay().getId(), scheduleIdFromToken);
        assertAgentCanManageParticipant(
                deletingParticipantId,
                proposal.getParticipant().getId(),
                scheduleIdFromToken
        );

        proposalRepository.delete(proposal);
    }

    private ScheduleDay findScheduleDayForToken(Long scheduleDayId, Long scheduleIdFromToken) {
        ScheduleDay scheduleDay = scheduleDayRepository.findByIdForSchedule(scheduleDayId, scheduleIdFromToken);
        if (scheduleDay == null) {
            throw forbidden("Schedule day is outside the schedule selected by the token");
        }
        return scheduleDay;
    }

    private void assertAgentCanManageParticipant(
            Long agentParticipantId,
            Long assignedParticipantId,
            Long scheduleId
    ) {
        if (participantRepository.countAssignedParticipantForAgentAndSchedule(
                agentParticipantId,
                assignedParticipantId,
                scheduleId
        ) == 0) {
            throw forbidden("Authenticated participant is not an agent of the requested participant in this schedule");
        }
    }

    private void validateProposalValues(CreateProposalRequest request, Long scheduleDayId) {
        Set<Integer> scheduleHours = scheduleEntryRepository.findHoursForScheduleDay(scheduleDayId).stream()
                .map(Short::intValue)
                .collect(Collectors.toSet());
        int hourStart = request.getHourStart();
        int hourEnd = request.getHourEnd();

        if (!scheduleHours.contains(hourStart) || !scheduleHours.contains(hourEnd)) {
            throw badRequest("Proposal hours must be configured schedule entry hours");
        }
        if (hourEnd < hourStart) {
            throw badRequest("hourEnd must be the same hour as or after hourStart");
        }

        int duration = hourEnd - hourStart + 1;
        int serviceLengthStart = request.getServiceLengthStart();
        int serviceLengthEnd = request.getServiceLengthEnd();

        if (serviceLengthStart < 1 || serviceLengthStart > duration) {
            throw badRequest("serviceLengthStart must be between 1 and proposal duration");
        }
        if (serviceLengthEnd < serviceLengthStart || serviceLengthEnd > duration) {
            throw badRequest("serviceLengthEnd must be between serviceLengthStart and proposal duration");
        }

        Integer breakLengthStart = request.getBreakLengthStart();
        Integer breakLengthEnd = request.getBreakLengthEnd();
        if ((breakLengthStart == null) != (breakLengthEnd == null)) {
            throw badRequest("breakLengthStart and breakLengthEnd must be provided together");
        }
        if (breakLengthStart == null) {
            return;
        }

        int maximumBreakLength = duration - serviceLengthStart;
        if (breakLengthStart < 1 || breakLengthStart >= maximumBreakLength) {
            throw badRequest("breakLengthStart must be at least 1 and less than the remaining proposal duration");
        }
        if (breakLengthEnd < breakLengthStart || breakLengthEnd > maximumBreakLength) {
            throw badRequest("breakLengthEnd must be between breakLengthStart and the remaining proposal duration");
        }
    }

    private Short toShort(Integer value) {
        return value == null ? null : value.shortValue();
    }

    private ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }

    private ResponseStatusException forbidden(String reason) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, reason);
    }

    private ResponseStatusException notFound(String reason) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, reason);
    }
}
