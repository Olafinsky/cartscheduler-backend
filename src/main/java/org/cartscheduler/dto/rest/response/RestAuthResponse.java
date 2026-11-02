package org.cartscheduler.dto.rest.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestAuthResponse {
    private String jwtToken;
    private Long expiresAt;
    private Long scheduleId;
    private boolean allowPairSelection;
    private boolean allowAlternativeGroupSelection;

    public RestAuthResponse(String jwtToken, Long expiresAt, Long scheduleId, boolean allowPairSelection) {
        this(jwtToken, expiresAt, scheduleId, allowPairSelection, false);
    }
}
