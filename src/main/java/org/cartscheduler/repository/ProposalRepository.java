package org.cartscheduler.repository;

import org.cartscheduler.entity.Proposal;
import org.cartscheduler.entity.ScheduleDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
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

    @Query("SELECT DISTINCT sourceDay FROM Proposal sourceProposal "
            + "JOIN sourceProposal.scheduleDay sourceDay "
            + "JOIN FETCH sourceDay.schedule sourceSchedule "
            + "JOIN sourceSchedule.accessibleParticipants assignedParticipant "
            + "JOIN assignedParticipant.agentParticipants agentParticipant "
            + "WHERE sourceProposal.participant.id = ?1 "
            + "AND assignedParticipant.id = ?1 "
            + "AND agentParticipant.id = ?2 "
            + "AND sourceDay.scheduleDate IS NOT NULL "
            + "AND sourceDay.scheduleDate < ?3 "
            + "AND sourceSchedule.id = ?4 "
            + "ORDER BY sourceDay.scheduleDate DESC, sourceDay.id DESC")
    List<ScheduleDay> findPrefillSourceDaysInSchedule(
            Long participantId,
            Long agentParticipantId,
            LocalDate targetDate,
            Long scheduleId
    );

    @Query("SELECT DISTINCT sourceDay FROM Proposal sourceProposal "
            + "JOIN sourceProposal.scheduleDay sourceDay "
            + "JOIN FETCH sourceDay.schedule sourceSchedule "
            + "JOIN sourceSchedule.accessibleParticipants assignedParticipant "
            + "JOIN assignedParticipant.agentParticipants agentParticipant "
            + "WHERE sourceProposal.participant.id = ?1 "
            + "AND assignedParticipant.id = ?1 "
            + "AND agentParticipant.id = ?2 "
            + "AND sourceDay.scheduleDate IS NOT NULL "
            + "AND sourceDay.scheduleDate < ?3 "
            + "AND sourceSchedule.id <> ?4 "
            + "ORDER BY sourceDay.scheduleDate DESC, sourceSchedule.id DESC, sourceDay.id DESC")
    List<ScheduleDay> findPrefillSourceDaysInEarlierSchedules(
            Long participantId,
            Long agentParticipantId,
            LocalDate targetDate,
            Long targetScheduleId
    );

    @Query("SELECT proposal FROM Proposal proposal "
            + "LEFT JOIN FETCH proposal.pairParticipant "
            + "WHERE proposal.participant.id = ?1 "
            + "AND proposal.scheduleDay.id = ?2 "
            + "ORDER BY proposal.hourStart ASC, proposal.hourEnd ASC, proposal.id ASC")
    List<Proposal> findForParticipantAndScheduleDayForPrefill(Long participantId, Long scheduleDayId);

    long countByParticipant_IdAndScheduleDay_Id(Long participantId, Long scheduleDayId);

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
