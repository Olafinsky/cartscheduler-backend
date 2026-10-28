package org.cartscheduler.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.cartscheduler.entity.Participant;
import org.cartscheduler.entity.Proposal;
import org.cartscheduler.entity.Schedule;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.repository.ParticipantRepository;
import org.cartscheduler.repository.ProposalRepository;
import org.cartscheduler.repository.ScheduleDayRepository;
import org.cartscheduler.repository.ScheduleRepository;
import org.cartscheduler.web.model.DashboardSummary;
import org.cartscheduler.web.model.ParticipantProposalRow;
import org.cartscheduler.web.model.ProposalCellDisplay;
import org.cartscheduler.web.model.ScheduleListItem;
import org.cartscheduler.web.model.ScheduleSummaryDay;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {
    private final ScheduleRepository scheduleRepository;
    private final ScheduleDayRepository scheduleDayRepository;
    private final ParticipantRepository participantRepository;
    private final ProposalRepository proposalRepository;

    public DashboardSummary getSummary(Long requestedScheduleId) {
        List<Schedule> schedules = scheduleRepository.findAll(Sort.by(Sort.Order.desc("id")));
        List<ScheduleListItem> scheduleItems = schedules.stream()
                .map(schedule -> new ScheduleListItem(
                        schedule.getId(),
                        schedule.getName(),
                        scheduleDayRepository.findForSchedule(schedule.getId()).size()
                ))
                .toList();

        Schedule selectedSchedule = selectSchedule(schedules, requestedScheduleId);
        if (selectedSchedule == null) {
            return new DashboardSummary(scheduleItems, null, null, List.of(), List.of());
        }

        List<ScheduleDay> scheduleDays = scheduleDayRepository.findForSchedule(selectedSchedule.getId()).stream()
                .sorted(scheduleDayComparator())
                .toList();
        List<ScheduleSummaryDay> days = scheduleDays.stream()
                .map(day -> new ScheduleSummaryDay(day.getId(), day.getName(), day.getScheduleDate()))
                .toList();

        Map<Long, Map<Long, List<ProposalCellDisplay>>> cellsByParticipant = new LinkedHashMap<>();
        List<Participant> participants = participantRepository.findAll(
                Sort.by(Sort.Order.asc("name").ignoreCase())
        );
        for (Participant participant : participants) {
            Map<Long, List<ProposalCellDisplay>> cells = new LinkedHashMap<>();
            for (ScheduleDay day : scheduleDays) {
                cells.put(day.getId(), new ArrayList<>());
            }
            cellsByParticipant.put(participant.getId(), cells);
        }

        List<Proposal> proposals = proposalRepository.findForSchedule(selectedSchedule.getId()).stream()
                .sorted(Comparator
                        .comparingLong((Proposal proposal) -> proposal.getScheduleDay().getId())
                        .thenComparingInt(Proposal::getHourStart)
                        .thenComparingInt(Proposal::getHourEnd)
                        .thenComparingLong(Proposal::getId))
                .toList();
        for (Proposal proposal : proposals) {
            Map<Long, List<ProposalCellDisplay>> participantCells = cellsByParticipant.get(
                    proposal.getParticipant().getId()
            );
            if (participantCells == null) {
                continue;
            }
            List<ProposalCellDisplay> cell = participantCells.get(proposal.getScheduleDay().getId());
            if (cell != null) {
                cell.add(toDisplay(proposal));
            }
        }
        List<ParticipantProposalRow> rows = new ArrayList<>();
        for (Participant participant : participants) {
            rows.add(new ParticipantProposalRow(
                    participant.getName(),
                    cellsByParticipant.get(participant.getId())
            ));
        }

        return new DashboardSummary(
                scheduleItems,
                selectedSchedule.getId(),
                selectedSchedule.getName(),
                days,
                rows
        );
    }

    private Schedule selectSchedule(List<Schedule> schedules, Long requestedScheduleId) {
        if (requestedScheduleId != null) {
            for (Schedule schedule : schedules) {
                if (schedule.getId() == requestedScheduleId) {
                    return schedule;
                }
            }
        }
        // The schedule list is loaded by descending ID, so the first item is
        // the most recently created schedule. Do not let an older schedule
        // win merely because it contains today's date.
        return schedules.isEmpty() ? null : schedules.getFirst();
    }

    private ProposalCellDisplay toDisplay(Proposal proposal) {
        String serviceLength = formatDuration(proposal.getServiceLengthStart(), proposal.getServiceLengthEnd());
        if (serviceLength == null) {
            short fallbackLength = (short) Math.max(1, proposal.getHourEnd() - proposal.getHourStart());
            serviceLength = formatDuration(fallbackLength, fallbackLength);
        }
        return new ProposalCellDisplay(
                proposal.getHourStart() + ":00-" + (proposal.getHourEnd() + 1) + ":00",
                serviceLength,
                formatDuration(proposal.getBreakLengthStart(), proposal.getBreakLengthEnd()),
                abbreviate(proposal.getAlternativeGroup(), 6),
                proposal.getPairParticipant() == null
                        ? null
                        : abbreviatePairParticipant(proposal.getPairParticipant().getName())
        );
    }

    private String formatDuration(Short start, Short end) {
        if (start == null || end == null) {
            return null;
        }
        return start.equals(end) ? start + "h" : start + "-" + end + "h";
    }

    private String abbreviate(String value, int maximumLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() <= maximumLength) {
            return normalized;
        }
        return normalized.substring(0, maximumLength - 1) + ".";
    }

    /**
     * Keep the 16-character name part used in the agreed example, followed by
     * an ellipsis dot when the full participant name does not fit.
     */
    private String abbreviatePairParticipant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() <= 16) {
            return normalized;
        }
        return normalized.substring(0, 16) + ".";
    }

    private Comparator<ScheduleDay> scheduleDayComparator() {
        return Comparator
                .comparing(
                        ScheduleDay::getScheduleDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                )
                .thenComparingInt(ScheduleDay::getDayOfWeek)
                .thenComparing(ScheduleDay::getName, String.CASE_INSENSITIVE_ORDER);
    }
}
