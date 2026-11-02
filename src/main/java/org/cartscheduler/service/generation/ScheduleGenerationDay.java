package org.cartscheduler.service.generation;

import java.util.List;
import org.cartscheduler.entity.ScheduleDay;
import org.cartscheduler.entity.ScheduleEntry;

/** A persisted schedule day together with its row order in the raw sheet. */
record ScheduleGenerationDay(int index, ScheduleDay day, List<ScheduleEntry> entries) {
    ScheduleGenerationDay {
        entries = List.copyOf(entries);
    }
}
