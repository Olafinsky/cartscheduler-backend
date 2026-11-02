package org.cartscheduler.dto.rest.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cartscheduler.entity.BreakMode;

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

    @Positive
    private Long pairParticipantId;

    @Min(0)
    @Max(2)
    private Integer breakMode;

    @Size(max = 64)
    private String alternativeGroup;

    /**
     * Compatibility constructor for callers of the API shape that included
     * pair and break preferences but predated alternative proposal groups.
     */
    public CreateProposalRequest(
            Long participantId,
            Long scheduleDayId,
            Integer hourStart,
            Integer hourEnd,
            Integer serviceLengthStart,
            Integer serviceLengthEnd,
            Integer breakLengthStart,
            Integer breakLengthEnd,
            Long pairParticipantId,
            Integer breakMode
    ) {
        this(
                participantId,
                scheduleDayId,
                hourStart,
                hourEnd,
                serviceLengthStart,
                serviceLengthEnd,
                breakLengthStart,
                breakLengthEnd,
                pairParticipantId,
                breakMode,
                null
        );
    }

    /**
     * Compatibility constructor for callers of the previous API shape. A
     * supplied break range used to mean that a break was required.
     */
    public CreateProposalRequest(
            Long participantId,
            Long scheduleDayId,
            Integer hourStart,
            Integer hourEnd,
            Integer serviceLengthStart,
            Integer serviceLengthEnd,
            Integer breakLengthStart,
            Integer breakLengthEnd
    ) {
        this(
                participantId,
                scheduleDayId,
                hourStart,
                hourEnd,
                serviceLengthStart,
                serviceLengthEnd,
                breakLengthStart,
                breakLengthEnd,
                null,
                (int) (breakLengthStart == null && breakLengthEnd == null
                        ? BreakMode.NO_BREAK.getCode()
                        : BreakMode.REQUIRE_BREAK.getCode()),
                null
        );
    }
}
