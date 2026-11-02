package org.cartscheduler.dto.rest.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.cartscheduler.entity.ScheduleDay;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleDayDto {
    private long id;
    private String name;
    private LocalDate date;
    private int firstHour;
    private int lastHour;

    private Boolean hasProposals;

    public ScheduleDayDto(long id, String name, int firstHour, int lastHour) {
        this.id = id;
        this.name = name;
        this.firstHour = firstHour;
        this.lastHour = lastHour;
    }

    public ScheduleDayDto(ScheduleDay scheduleDay) {
        id = scheduleDay.getId();
        name = scheduleDay.getName();
        date = scheduleDay.getScheduleDate();
        firstHour = scheduleDay.getScheduleEntries().getFirst().getHour();
        lastHour = scheduleDay.getScheduleEntries().getLast().getHour();
    }

    public ScheduleDayDto(ScheduleDay scheduleDay, boolean hasProposals) {
        this(scheduleDay);
        this.hasProposals = hasProposals;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Boolean getHasProposals() {
        return hasProposals;
    }
}
