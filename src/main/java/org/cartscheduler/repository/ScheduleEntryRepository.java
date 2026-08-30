package org.cartscheduler.repository;

import org.cartscheduler.entity.ScheduleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScheduleEntryRepository extends JpaRepository<ScheduleEntry, Long> {

    @Query("SELECT scheduleEntry.hour FROM ScheduleEntry scheduleEntry " +
            "WHERE scheduleEntry.scheduleDay.id = ?1")
    List<Short> findHoursForScheduleDay(Long scheduleDayId);
}
