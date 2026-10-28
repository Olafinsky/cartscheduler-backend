package org.cartscheduler.repository;

import org.cartscheduler.entity.ScheduleAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleAccessRepository extends JpaRepository<ScheduleAccess, Long> {

    List<ScheduleAccess> findBySchedule_IdOrderByParticipant_NameAsc(long scheduleId);

    long deleteBySchedule_Id(long scheduleId);
}
