package org.cartscheduler.repository;

import org.cartscheduler.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    @Query("SELECT proposal FROM Proposal proposal " +
            "JOIN FETCH proposal.participant " +
            "JOIN FETCH proposal.scheduleDay scheduleDay " +
            "LEFT JOIN FETCH proposal.pairParticipant " +
            "WHERE scheduleDay.schedule.id = ?1")
    List<Proposal> findForSchedule(Long scheduleId);

    @Query("SELECT proposal FROM Proposal proposal " +
            "WHERE proposal.participant.id = ?1 " +
            "AND proposal.scheduleDay.id = ?2 " +
            "ORDER BY proposal.dateAdd ASC, proposal.id ASC")
    List<Proposal> findForParticipantAndScheduleDay(Long participantId, Long scheduleDayId);

    @Query("SELECT DISTINCT proposal.participant.id FROM Proposal proposal "
            + "WHERE proposal.scheduleDay.schedule.id = ?1 "
            + "AND proposal.participant.id IN ?2")
    List<Long> findParticipantIdsWithProposalsForSchedule(
            Long scheduleId,
            Collection<Long> participantIds
    );

    @Query("SELECT DISTINCT proposal.scheduleDay.id FROM Proposal proposal "
            + "WHERE proposal.participant.id = ?1 "
            + "AND proposal.scheduleDay.schedule.id = ?2")
    List<Long> findScheduleDayIdsWithProposalsForParticipantAndSchedule(
            Long participantId,
            Long scheduleId
    );
}
