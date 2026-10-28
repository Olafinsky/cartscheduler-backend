package org.cartscheduler.web.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** A safely-rendered fragment of one proposal in the administration matrix. */
@Getter
@AllArgsConstructor
public class ProposalCellDisplay {
    private final String hours;
    private final String serviceLength;
    private final String breakLength;
    private final String alternativeGroup;
    private final String pairParticipantName;
}
