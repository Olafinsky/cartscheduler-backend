package org.cartscheduler;

import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.entity.ScheduleEntry;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.repository.ScheduleDayRepository;
import org.cartscheduler.repository.ScheduleEntryRepository;
import org.cartscheduler.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Mocker {

    @Autowired
    private ScheduleDayRepository scheduleDayRepository;

    @Autowired
    private ScheduleEntryRepository scheduleEntryRepository;


    public void mock() {
        for (ScheduleDay day : scheduleDayRepository.findAll()) {
            for (short i = 7; i < 18; i++) {
                ScheduleEntry e = new ScheduleEntry();
                e.setHour(i);
                e.setScheduleDay(day);
                e.setName(i + ":00");
                scheduleEntryRepository.save(e);
            }
        }
    }


}
