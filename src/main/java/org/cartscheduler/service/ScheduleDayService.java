package org.cartscheduler.service;

import org.cartscheduler.dto.rest.response.ScheduleDayDto;
import org.cartscheduler.dto.rest.response.ScheduleDto;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.repository.ScheduleDayRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScheduleDayService {

    @Autowired
    private ScheduleDayRepository scheduleDayRepository;

    public List<ScheduleDayDto> prepareScheduleDayDtoListForSchedule(Long id) {
        List<ScheduleDayDto> scheduleDays = new ArrayList<>();
        for (ScheduleDay scheduleDay : scheduleDayRepository.findForSchedule(id)) {
            scheduleDays.add(new ScheduleDayDto(scheduleDay));
        }
        return scheduleDays;
    }

    public ScheduleDayDto prepareScheduleDayDtoForParticipant(Long id, Long scheduleId) {
        ScheduleDay scheduleDay = scheduleDayRepository.findByIdForSchedule(id, scheduleId);
        if (scheduleDay == null) {
            return null;
        }
        return new ScheduleDayDto(scheduleDay);
    }
}
