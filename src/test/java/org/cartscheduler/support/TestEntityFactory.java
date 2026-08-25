package org.cartscheduler.support;

import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.entity.ScheduleEntry;
import org.cartscheduler.impl.RestUserDetails;

import java.util.List;

public final class TestEntityFactory {

    private TestEntityFactory() {
    }

    public static Participant participant(long id) {
        Participant participant = new Participant();
        participant.setId(id);
        participant.setName("Participant " + id);
        participant.setEmail("participant" + id + "@example.test");
        return participant;
    }

    public static RestUserDetails user(long participantId) {
        return new RestUserDetails(participant(participantId));
    }

    public static Schedule schedule(long id, String name, List<Participant> accessibleParticipants) {
        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setName(name);
        schedule.setAccessibleParticipants(accessibleParticipants);
        return schedule;
    }

    public static ScheduleDay scheduleDay(long id, String name, List<ScheduleEntry> entries) {
        ScheduleDay scheduleDay = new ScheduleDay();
        scheduleDay.setId(id);
        scheduleDay.setName(name);
        scheduleDay.setScheduleEntries(entries);
        return scheduleDay;
    }

    public static ScheduleEntry scheduleEntry(long id, String name, short hour) {
        ScheduleEntry entry = new ScheduleEntry();
        entry.setId(id);
        entry.setName(name);
        entry.setHour(hour);
        return entry;
    }
}
