package org.cartscheduler.service;

import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.repository.ScheduleDayRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleDayService {

    @Autowired
    private ScheduleDayRepository scheduleDayRepository;

    public List<ScheduleDay> findForSchedule(Long id) {
        return scheduleDayRepository.findForSchedule(id);
    }
}
