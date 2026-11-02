package org.cartscheduler.dto.rest.response;

import java.time.DayOfWeek;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProposalPrefillSourceDto {
    private long scheduleId;
    private long scheduleDayId;
    private LocalDate date;
    private DayOfWeek dayOfWeek;
    private long proposalId;
    private int proposalIndex;
    private int proposalCount;
}
