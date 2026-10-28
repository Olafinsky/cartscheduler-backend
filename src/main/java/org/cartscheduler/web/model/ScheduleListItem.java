package org.cartscheduler.web.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScheduleListItem {
    private final long id;
    private final String name;
    private final int dayCount;
}
