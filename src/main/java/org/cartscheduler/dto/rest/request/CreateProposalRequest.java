package org.cartscheduler.dto.rest.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateProposalRequest {

    @NotNull
    @Positive
    private Long participantId;

    @NotNull
    @Positive
    private Long scheduleDayId;

    @NotNull
    private Integer hourStart;

    @NotNull
    private Integer hourEnd;

    @NotNull
    @Min(1)
    private Integer serviceLengthStart;

    @NotNull
    @Min(1)
    private Integer serviceLengthEnd;

    @Min(1)
    private Integer breakLengthStart;

    @Min(1)
    private Integer breakLengthEnd;
}
