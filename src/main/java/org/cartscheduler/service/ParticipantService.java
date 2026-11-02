package org.cartscheduler.service;

import org.cartscheduler.dto.rest.response.ParticipantDto;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.repository.ProposalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ParticipantService {
    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    public List<ParticipantDto> prepareAssignedParticipantDtoListForAgentParticipantAndSchedule(Long participantId, Long scheduleId) {
        List<ParticipantDto> participants = new ArrayList<>();
        for (Participant participant : participantRepository.findAssignedParticipants(participantId, scheduleId)) {
            participants.add(new ParticipantDto(participant));
        }
        return participants;
    }

    @Transactional(readOnly = true)
    public List<ParticipantDto> prepareAssignedParticipantDtoListWithProposalStatusForAgentParticipantAndSchedule(
            Long agentParticipantId,
            Long scheduleId
    ) {
        List<Participant> assignedParticipants = participantRepository.findAssignedParticipants(
                agentParticipantId,
                scheduleId
        );
        if (assignedParticipants.isEmpty()) {
            return List.of();
        }

        Set<Long> participantIds = new HashSet<>();
        for (Participant participant : assignedParticipants) {
            participantIds.add(participant.getId());
        }
        Set<Long> participantIdsWithProposals = new HashSet<>(
                proposalRepository.findParticipantIdsWithProposalsForSchedule(scheduleId, participantIds)
        );

        List<ParticipantDto> participants = new ArrayList<>();
        for (Participant participant : assignedParticipants) {
            participants.add(new ParticipantDto(
                    participant,
                    participantIdsWithProposals.contains(participant.getId())
            ));
        }
        return participants;
    }

    @Transactional(readOnly = true)
    public void assertAgentCanManageParticipantForSchedule(
            Long agentParticipantId,
            Long assignedParticipantId,
            Long scheduleId
    ) {
        if (participantRepository.countAssignedParticipantForAgentAndSchedule(
                agentParticipantId,
                assignedParticipantId,
                scheduleId
        ) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Authenticated participant is not an agent of the requested participant in this schedule"
            );
        }
    }
}
