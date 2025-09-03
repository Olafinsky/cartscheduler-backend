package org.cartscheduler.service;

import org.cartscheduler.dto.rest.response.ParticipantDto;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.repository.ParticipantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ParticipantService {
    @Autowired
    private ParticipantRepository participantRepository;

    public List<ParticipantDto> prepareAssignedParticipantDtoListForAgentParticipantAndSchedule(Long participantId, Long scheduleId) {
        List<ParticipantDto> participants = new ArrayList<>();
        for (Participant participant : participantRepository.findAssignedParticipantForAgentParticipantAndSchedule(participantId, scheduleId)) {
            participants.add(new ParticipantDto(participant));
        }
        return participants;
    }
}
