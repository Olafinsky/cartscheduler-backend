package org.cartscheduler.dto.rest.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response for read-only proposal-form prefill suggestions. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProposalPrefillDto {
    private boolean available;
    private ProposalPrefillSourceDto source;
    private ProposalPrefillValuesDto values;

    public static ProposalPrefillDto unavailable() {
        return new ProposalPrefillDto(false, null, null);
    }
}
