package org.cartscheduler.dto.rest.response;

import lombok.*;
import org.cartscheduler.entity.ScheduleDay;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleDayDto {
    private long id;
    private String name;
    private int firstHour;
    private int lastHour;

    public ScheduleDayDto(ScheduleDay scheduleDay) {
        id = scheduleDay.getId();
        name = scheduleDay.getName();
        firstHour = scheduleDay.getScheduleEntries().getFirst().getHour();
        lastHour = scheduleDay.getScheduleEntries().getLast().getHour();
    }
}
