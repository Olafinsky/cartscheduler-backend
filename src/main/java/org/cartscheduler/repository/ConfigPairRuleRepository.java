package org.cartscheduler.repository;

import java.util.List;
import java.util.Optional;
import org.cartscheduler.entity.ConfigPairRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConfigPairRuleRepository extends JpaRepository<ConfigPairRule, Long> {
    Optional<ConfigPairRule> findByParticipant1_IdAndParticipant2_Id(long participant1Id, long participant2Id);

    /** See {@link ConfigParticipantRuleRepository#findAllWithParticipant()}. */
    @Query("""
            select rule
            from ConfigPairRule rule
            join fetch rule.participant1 participant1
            join fetch rule.participant2 participant2
            order by lower(participant1.name), lower(participant2.name), rule.id
            """)
    List<ConfigPairRule> findAllWithParticipants();

    @Query("""
            select rule
            from ConfigPairRule rule
            join fetch rule.participant1
            join fetch rule.participant2
            where rule.id = :id
            """)
    Optional<ConfigPairRule> findWithParticipantsById(@Param("id") long id);
}
