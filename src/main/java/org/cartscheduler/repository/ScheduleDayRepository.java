package org.cartscheduler.repository;

import org.cartscheduler.entity.ScheduleDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScheduleDayRepository extends JpaRepository<ScheduleDay, Long> {
    @Query("SELECT sd FROM ScheduleDay sd WHERE sd.schedule.id = ?1")
    List<ScheduleDay> findForSchedule(Long id);

    @Query("SELECT DISTINCT sd FROM ScheduleDay sd "
            + "LEFT JOIN FETCH sd.scheduleEntries scheduleEntry "
            + "LEFT JOIN FETCH scheduleEntry.participant1 "
            + "LEFT JOIN FETCH scheduleEntry.participant2 "
            + "WHERE sd.schedule.id = ?1")
    List<ScheduleDay> findForScheduleWithEntries(Long id);

    @Query("SELECT sd FROM ScheduleDay sd WHERE sd.id = ?1 AND sd.schedule.id = ?2")
    ScheduleDay findByIdForSchedule(Long id, Long scheduleId);

}
