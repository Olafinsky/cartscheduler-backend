package org.cartscheduler.dto.rest.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProposalPrefillValuesDto {
    private Integer hourStart;
    private Integer hourEnd;
    private Integer serviceLengthStart;
    private Integer serviceLengthEnd;
    private Integer breakMode;
    private Integer breakLengthStart;
    private Integer breakLengthEnd;
    private String alternativeGroup;
    private Long pairParticipantId;
}
