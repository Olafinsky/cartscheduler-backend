package org.cartscheduler.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.dto.rest.response.ProposalPrefillDto;
import org.cartscheduler.dto.rest.response.ProposalPrefillSourceDto;
import org.cartscheduler.dto.rest.response.ProposalPrefillValuesDto;
import org.cartscheduler.entity.BreakMode;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.Proposal;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.repository.ProposalRepository;
import org.cartscheduler.repository.ScheduleDayRepository;
import org.cartscheduler.repository.ScheduleEntryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Provides a read-only suggestion for filling a proposal form from the most
 * recent compatible proposal for the same calendar weekday.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProposalPrefillService {
    private static final Comparator<Proposal> SOURCE_PROPOSAL_ORDER = Comparator
            .comparingInt(Proposal::getHourStart)
            .thenComparingInt(Proposal::getHourEnd)
            .thenComparingLong(Proposal::getId);

    private static final Comparator<ScheduleDay> SOURCE_DAY_ORDER = Comparator
            .comparing(ScheduleDay::getScheduleDate)
            .thenComparingLong(ScheduleDay::getId);

    private final ProposalRepository proposalRepository;
    private final ParticipantRepository participantRepository;
    private final ScheduleDayRepository scheduleDayRepository;
    private final ScheduleEntryRepository scheduleEntryRepository;
    private final ParticipantService participantService;

    public ProposalPrefillDto preparePrefill(
            Long participantId,
            Long targetScheduleDayId,
            Long agentParticipantId,
            Long scheduleIdFromToken
    ) {
        ScheduleDay targetDay = findScheduleDayForToken(targetScheduleDayId, scheduleIdFromToken);
        participantService.assertAgentCanManageParticipantForSchedule(
                agentParticipantId,
                participantId,
                scheduleIdFromToken
        );
        Participant insertingParticipant = participantRepository.findById(agentParticipantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated participant does not exist"
                ));

        LocalDate targetDate = targetDay.getScheduleDate();
        if (targetDate == null) {
            return ProposalPrefillDto.unavailable();
        }

        ScheduleDay sourceDay = findSourceDay(
                participantId,
                agentParticipantId,
                scheduleIdFromToken,
                targetDate
        );
        if (sourceDay == null) {
            return ProposalPrefillDto.unavailable();
        }

        List<Proposal> sourceProposals = new ArrayList<>(
                proposalRepository.findForParticipantAndScheduleDayForPrefill(participantId, sourceDay.getId())
        );
        sourceProposals.sort(SOURCE_PROPOSAL_ORDER);
        if (sourceProposals.isEmpty()) {
            return ProposalPrefillDto.unavailable();
        }

        long targetProposalCount = proposalRepository.countByParticipant_IdAndScheduleDay_Id(
                participantId,
                targetScheduleDayId
        );
        int sourceProposalIndex = (int) Math.min(targetProposalCount, sourceProposals.size() - 1L);
        Proposal sourceProposal = sourceProposals.get(sourceProposalIndex);

        Set<Integer> targetHours = scheduleEntryRepository.findHoursForScheduleDay(targetScheduleDayId).stream()
                .map(Short::intValue)
                .collect(Collectors.toSet());
        if (!isCompatibleWithTargetForm(sourceProposal, targetHours, insertingParticipant)) {
            return ProposalPrefillDto.unavailable();
        }

        return new ProposalPrefillDto(
                true,
                new ProposalPrefillSourceDto(
                        sourceDay.getSchedule().getId(),
                        sourceDay.getId(),
                        sourceDay.getScheduleDate(),
                        sourceDay.getScheduleDate().getDayOfWeek(),
                        sourceProposal.getId(),
                        sourceProposalIndex,
                        sourceProposals.size()
                ),
                valuesFor(sourceProposal, participantId, agentParticipantId, scheduleIdFromToken, insertingParticipant)
        );
    }

    private ScheduleDay findScheduleDayForToken(Long scheduleDayId, Long scheduleIdFromToken) {
        ScheduleDay scheduleDay = scheduleDayRepository.findByIdForSchedule(scheduleDayId, scheduleIdFromToken);
        if (scheduleDay == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Schedule day is outside the schedule selected by the token"
            );
        }
        return scheduleDay;
    }

    private ScheduleDay findSourceDay(
            Long participantId,
            Long agentParticipantId,
            Long targetScheduleId,
            LocalDate targetDate
    ) {
        Optional<ScheduleDay> sourceInTargetSchedule = nearestSameWeekdayBefore(
                proposalRepository.findPrefillSourceDaysInSchedule(
                        participantId,
                        agentParticipantId,
                        targetDate,
                        targetScheduleId
                ),
                targetDate
        );
        if (sourceInTargetSchedule.isPresent()) {
            return sourceInTargetSchedule.get();
        }

        return nearestSameWeekdayBefore(
                proposalRepository.findPrefillSourceDaysInEarlierSchedules(
                        participantId,
                        agentParticipantId,
                        targetDate,
                        targetScheduleId
                ),
                targetDate
        ).orElse(null);
    }

    private Optional<ScheduleDay> nearestSameWeekdayBefore(
            List<ScheduleDay> candidates,
            LocalDate targetDate
    ) {
        DayOfWeek targetDayOfWeek = targetDate.getDayOfWeek();
        return candidates.stream()
                .filter(candidate -> candidate.getScheduleDate() != null)
                .filter(candidate -> candidate.getScheduleDate().isBefore(targetDate))
                .filter(candidate -> candidate.getScheduleDate().getDayOfWeek() == targetDayOfWeek)
                .max(SOURCE_DAY_ORDER);
    }

    private boolean isCompatibleWithTargetForm(
            Proposal sourceProposal,
            Set<Integer> targetHours,
            Participant insertingParticipant
    ) {
        int hourStart = sourceProposal.getHourStart();
        int hourEnd = sourceProposal.getHourEnd();
        if (!targetHours.contains(hourStart) || !targetHours.contains(hourEnd) || hourEnd < hourStart) {
            return false;
        }

        Short sourceServiceLengthStart = sourceProposal.getServiceLengthStart();
        Short sourceServiceLengthEnd = sourceProposal.getServiceLengthEnd();
        BreakMode breakMode = sourceProposal.getBreakMode();
        if (sourceServiceLengthStart == null || sourceServiceLengthEnd == null || breakMode == null) {
            return false;
        }

        int duration = hourEnd - hourStart + 1;
        int serviceLengthStart = sourceServiceLengthStart;
        int serviceLengthEnd = sourceServiceLengthEnd;
        if (serviceLengthStart < 1 || serviceLengthStart > duration
                || serviceLengthEnd < serviceLengthStart || serviceLengthEnd > duration) {
            return false;
        }

        if (insertingParticipant.isAllowAlternativeGroupSelection()
                && sourceProposal.getAlternativeGroup() != null
                && sourceProposal.getAlternativeGroup().length() > 64) {
            return false;
        }

        if (breakMode == BreakMode.NO_BREAK) {
            return true;
        }

        Short sourceBreakLengthStart = sourceProposal.getBreakLengthStart();
        Short sourceBreakLengthEnd = sourceProposal.getBreakLengthEnd();
        if ((sourceBreakLengthStart == null) != (sourceBreakLengthEnd == null)) {
            return false;
        }
        if (breakMode == BreakMode.REQUIRE_BREAK && sourceBreakLengthStart == null) {
            return false;
        }
        if (sourceBreakLengthStart == null) {
            return true;
        }

        int maximumBreakLength = duration - serviceLengthStart;
        int breakLengthStart = sourceBreakLengthStart;
        int breakLengthEnd = sourceBreakLengthEnd;
        return breakLengthStart >= 1
                && breakLengthStart < maximumBreakLength
                && breakLengthEnd >= breakLengthStart
                && breakLengthEnd <= maximumBreakLength;
    }

    private ProposalPrefillValuesDto valuesFor(
            Proposal sourceProposal,
            Long targetParticipantId,
            Long agentParticipantId,
            Long targetScheduleId,
            Participant insertingParticipant
    ) {
        BreakMode breakMode = sourceProposal.getBreakMode();
        boolean noBreak = breakMode == BreakMode.NO_BREAK;
        Long pairParticipantId = mappedPairParticipantId(
                sourceProposal,
                targetParticipantId,
                agentParticipantId,
                targetScheduleId,
                insertingParticipant
        );
        return new ProposalPrefillValuesDto(
                (int) sourceProposal.getHourStart(),
                (int) sourceProposal.getHourEnd(),
                sourceProposal.getServiceLengthStart().intValue(),
                sourceProposal.getServiceLengthEnd().intValue(),
                (int) breakMode.getCode(),
                noBreak || sourceProposal.getBreakLengthStart() == null
                        ? null
                        : sourceProposal.getBreakLengthStart().intValue(),
                noBreak || sourceProposal.getBreakLengthEnd() == null
                        ? null
                        : sourceProposal.getBreakLengthEnd().intValue(),
                insertingParticipant.isAllowAlternativeGroupSelection()
                        ? sourceProposal.getAlternativeGroup()
                        : null,
                pairParticipantId
        );
    }

    private Long mappedPairParticipantId(
            Proposal sourceProposal,
            Long targetParticipantId,
            Long agentParticipantId,
            Long targetScheduleId,
            Participant insertingParticipant
    ) {
        if (!insertingParticipant.isAllowPairSelection() || sourceProposal.getPairParticipant() == null) {
            return null;
        }

        long sourcePairParticipantId = sourceProposal.getPairParticipant().getId();
        if (sourcePairParticipantId == targetParticipantId) {
            return null;
        }

        return participantRepository.countAssignedParticipantForAgentAndSchedule(
                agentParticipantId,
                sourcePairParticipantId,
                targetScheduleId
        ) > 0 ? sourcePairParticipantId : null;
    }
}
