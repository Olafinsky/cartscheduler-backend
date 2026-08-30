package org.cartscheduler.repository;

import org.cartscheduler.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    @Query("SELECT s FROM Schedule s JOIN s.accessibleParticipants p WHERE p.id = ?1")
    List<Schedule> findForParticipant(Long id);

    @Query("SELECT DISTINCT schedule FROM Schedule schedule " +
            "JOIN schedule.accessibleParticipants participant " +
            "JOIN participant.agentParticipants agent " +
            "WHERE agent.id = ?1")
    List<Schedule> findForAgent(Long agentParticipantId);
}
