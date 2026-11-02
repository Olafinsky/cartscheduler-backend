package org.cartscheduler.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.cartscheduler.entity.ConfigScheduleRestrictedDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigScheduleRestrictedDateRepository
        extends JpaRepository<ConfigScheduleRestrictedDate, Long> {
    List<ConfigScheduleRestrictedDate> findByRestrictedDateBetween(LocalDate startDate, LocalDate endDate);

    Optional<ConfigScheduleRestrictedDate> findByRestrictedDate(LocalDate restrictedDate);
}
