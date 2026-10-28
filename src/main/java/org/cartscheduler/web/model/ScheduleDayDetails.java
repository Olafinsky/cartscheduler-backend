package org.cartscheduler.web.model;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScheduleDayDetails {
    private final String name;
    private final LocalDate date;
    private final short dayOfWeek;
    private final List<Short> serviceHours;
}
