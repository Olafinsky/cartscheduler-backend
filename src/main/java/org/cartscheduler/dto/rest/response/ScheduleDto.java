package org.cartscheduler.dto.rest.response;

import lombok.*;
import org.cartscheduler.entity.Schedule;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleDto {
    private long id;
    private String name;

    public ScheduleDto(Schedule schedule) {
        this.name = schedule.getName();
        this.id = schedule.getId();
    }
}
