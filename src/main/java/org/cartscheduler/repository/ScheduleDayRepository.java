package org.cartscheduler.repository;

import org.cartscheduler.entity.ScheduleDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScheduleDayRepository extends JpaRepository<ScheduleDay, Long> {
    @Query("SELECT sd FROM ScheduleDay sd WHERE sd.schedule.id = ?1")
    List<ScheduleDay> findForSchedule(Long id);
}
