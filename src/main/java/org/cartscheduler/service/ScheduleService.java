package org.cartscheduler.service;

import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.impl.RestUserDetails;
import org.cartscheduler.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;


    public List<Schedule> findForParticipant(Long id) {
        return scheduleRepository.findForParticipant(id);
    }

    public boolean checkScheduleAccess(RestUserDetails userDetails, long scheduleId) {
        Optional<Schedule> queryResult = scheduleRepository.findById(scheduleId);
        if (queryResult.isEmpty()) {
            return false;
        }

        Schedule schedule = queryResult.get();
        for (Participant participant : schedule.getAccessibleParticipants()) {
            if (participant.getId() == userDetails.getId()) {
                return true;
            }
        }
        return false;
    }

}
