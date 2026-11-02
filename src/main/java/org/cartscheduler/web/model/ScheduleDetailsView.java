package org.cartscheduler.web.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScheduleDetailsView {
    private final long id;
    private final String name;
    private final List<ScheduleDayDetails> days;
}
