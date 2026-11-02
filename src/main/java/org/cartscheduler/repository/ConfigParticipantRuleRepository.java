package org.cartscheduler.repository;

import java.util.List;
import java.util.Optional;
import org.cartscheduler.entity.ConfigParticipantRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConfigParticipantRuleRepository extends JpaRepository<ConfigParticipantRule, Long> {
    Optional<ConfigParticipantRule> findByParticipant_Id(long participantId);

    /**
     * Administration views render participant data after the service transaction has ended.
     * Fetch it here deliberately because Open Session in View is disabled.
     */
    @Query("""
            select rule
            from ConfigParticipantRule rule
            join fetch rule.participant participant
            order by lower(participant.name), rule.id
            """)
    List<ConfigParticipantRule> findAllWithParticipant();

    @Query("""
            select rule
            from ConfigParticipantRule rule
            join fetch rule.participant
            where rule.id = :id
            """)
    Optional<ConfigParticipantRule> findWithParticipantById(@Param("id") long id);
}
