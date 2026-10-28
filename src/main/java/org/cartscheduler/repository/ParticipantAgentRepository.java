package org.cartscheduler.repository;

import java.util.List;
import org.cartscheduler.entity.ParticipantAgent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantAgentRepository extends JpaRepository<ParticipantAgent, Long> {
    List<ParticipantAgent> findByAgentParticipant_IdOrderByTargetParticipant_NameAsc(long agentParticipantId);

    List<ParticipantAgent> findByTargetParticipant_IdOrderByAgentParticipant_NameAsc(long targetParticipantId);

    long deleteByAgentParticipant_Id(long agentParticipantId);

    long deleteByTargetParticipant_Id(long targetParticipantId);
}
