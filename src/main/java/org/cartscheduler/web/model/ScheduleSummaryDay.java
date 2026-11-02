package org.cartscheduler.web.model;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScheduleSummaryDay {
    private final long id;
    private final String name;
    private final LocalDate date;
}
