package org.cartscheduler.repository;

import java.util.List;
import java.util.Optional;
import org.cartscheduler.entity.ConfigScheduleDay;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigScheduleDayRepository extends JpaRepository<ConfigScheduleDay, Long> {
    List<ConfigScheduleDay> findByActiveTrueOrderByDayOfWeekAsc();

    Optional<ConfigScheduleDay> findByDayOfWeek(short dayOfWeek);
}
