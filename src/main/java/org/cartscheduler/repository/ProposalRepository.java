package org.cartscheduler.repository;

import org.cartscheduler.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    @Query("SELECT proposal FROM Proposal proposal " +
            "WHERE proposal.participant.id = ?1 " +
            "AND proposal.scheduleDay.id = ?2 " +
            "ORDER BY proposal.dateAdd ASC, proposal.id ASC")
    List<Proposal> findForParticipantAndScheduleDay(Long participantId, Long scheduleDayId);
}
