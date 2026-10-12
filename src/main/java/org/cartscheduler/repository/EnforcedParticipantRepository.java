package org.cartscheduler.repository;

import java.util.Collection;
import java.util.List;
import org.cartscheduler.entity.EnforcedParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnforcedParticipantRepository extends JpaRepository<EnforcedParticipant, Long> {
    List<EnforcedParticipant> findByDayNameIn(Collection<String> dayNames);
}
