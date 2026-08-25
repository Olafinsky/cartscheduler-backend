package org.cartscheduler.service;

import org.cartscheduler.dto.rest.response.ScheduleDto;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.impl.RestUserDetails;
import org.cartscheduler.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;


    public List<ScheduleDto> prepareScheduleDtoListForParticipant(Long id) {
        List<ScheduleDto> schedules = new ArrayList<>();
        for (Schedule schedule : scheduleRepository.findForParticipant(id)) {
            schedules.add(new ScheduleDto(schedule));
        }
        return schedules;
    }

    public ScheduleDto prepareScheduleDtoForParticipant(Long participantId, Long scheduleId) {
        return scheduleRepository.findForParticipant(participantId).stream()
                .filter(schedule -> schedule.getId() == scheduleId)
                .findFirst()
                .map(ScheduleDto::new)
                .orElse(null);
    }

    public boolean checkScheduleAccess(RestUserDetails userDetails, long scheduleId) {
        Optional<Schedule> queryResult = scheduleRepository.findById(scheduleId);
        if (queryResult.isEmpty()) {
            return false;
        }

        Schedule schedule = queryResult.get();
        if (schedule.getAccessibleParticipants() == null) {
            return false;
        }

        return schedule.getAccessibleParticipants().stream()
                .anyMatch(participant -> participant.getId() == userDetails.getId());
    }

}
