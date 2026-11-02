package org.cartscheduler.web.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardSummary {
    private final List<ScheduleListItem> schedules;
    private final Long selectedScheduleId;
    private final String selectedScheduleName;
    private final List<ScheduleSummaryDay> days;
    private final List<ParticipantProposalRow> participants;

    public boolean hasSelectedSchedule() {
        return selectedScheduleId != null;
    }
}
