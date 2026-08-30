package org.cartscheduler.repository;

import org.cartscheduler.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    Optional<Participant> findByEmail(String email);

    @Query("SELECT p FROM Participant p JOIN p.agentParticipants ap JOIN p.schedules s WHERE ap.id = ?1 AND s.id  = ?2")
    List<Participant> findAssignedParticipants(Long participantId, Long scheduleId);

    @Query("SELECT COUNT(participant) FROM Participant participant " +
            "JOIN participant.agentParticipants agent " +
            "JOIN participant.schedules schedule " +
            "WHERE agent.id = ?1 AND schedule.id = ?2")
    long countAssignedParticipantsForAgentAndSchedule(Long agentParticipantId, Long scheduleId);

    @Query("SELECT COUNT(participant) FROM Participant participant " +
            "JOIN participant.agentParticipants agent " +
            "JOIN participant.schedules schedule " +
            "WHERE agent.id = ?1 AND participant.id = ?2 AND schedule.id = ?3")
    long countAssignedParticipantForAgentAndSchedule(
            Long agentParticipantId,
            Long assignedParticipantId,
            Long scheduleId
    );
}
