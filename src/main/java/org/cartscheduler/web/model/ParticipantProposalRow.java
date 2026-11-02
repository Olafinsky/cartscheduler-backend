package org.cartscheduler.web.model;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ParticipantProposalRow {
    private final String participantName;
    private final Map<Long, List<ProposalCellDisplay>> proposalsByDay;
}
