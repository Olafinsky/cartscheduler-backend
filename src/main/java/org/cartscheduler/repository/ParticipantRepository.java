package org.cartscheduler.repository;

import org.cartscheduler.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    Optional<Participant> findByEmail(String email);

    @Query("SELECT p FROM Participant p JOIN p.agentParticipants ap JOIN p.schedules s WHERE ap.id = ?1 AND s.id  = ?2")
    List<Participant> findAssignedParticipantForAgentParticipantAndSchedule(Long participantId, Long scheduleId);
}
