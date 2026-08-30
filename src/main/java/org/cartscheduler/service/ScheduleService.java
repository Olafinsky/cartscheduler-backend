package org.cartscheduler.service;

import org.cartscheduler.dto.rest.response.ScheduleDto;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.impl.RestUserDetails;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    public List<ScheduleDto> prepareScheduleDtoListForAgentParticipant(Long agentParticipantId) {
        List<ScheduleDto> schedules = new ArrayList<>();
        for (Schedule schedule : scheduleRepository.findForAgent(agentParticipantId)) {
            schedules.add(new ScheduleDto(schedule));
        }
        return schedules;
    }

    public ScheduleDto prepareScheduleDtoForSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .map(ScheduleDto::new)
                .orElse(null);
    }

    public boolean checkScheduleAccess(RestUserDetails userDetails, long scheduleId) {
        return participantRepository.countAssignedParticipantsForAgentAndSchedule(
                userDetails.getId(),
                scheduleId
        ) > 0;
    }

}
