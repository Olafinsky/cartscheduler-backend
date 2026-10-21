package org.cartscheduler.dto.rest.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cartscheduler.entity.BreakMode;
import org.cartscheduler.entity.Proposal;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProposalDto {

    private long id;
    private Date dateAdd;
    private long participantId;
    private Long pairParticipantId;
    private Long insertingParticipantId;
    private long scheduleDayId;
    private int hourStart;
    private int hourEnd;
    private int breakMode;
    private Short breakLengthStart;
    private Short breakLengthEnd;
    private Short serviceLengthStart;
    private Short serviceLengthEnd;
    private String alternativeGroup;

    public ProposalDto(Proposal proposal) {
        id = proposal.getId();
        dateAdd = proposal.getDateAdd();
        participantId = proposal.getParticipant().getId();
        pairParticipantId = proposal.getPairParticipant() == null ? null : proposal.getPairParticipant().getId();
        insertingParticipantId = proposal.getInsertingParticipant() == null
                ? null
                : proposal.getInsertingParticipant().getId();
        scheduleDayId = proposal.getScheduleDay().getId();
        hourStart = proposal.getHourStart();
        hourEnd = proposal.getHourEnd();
        breakMode = proposal.getBreakMode() == null
                ? BreakMode.INDIFFERENT.getCode()
                : proposal.getBreakMode().getCode();
        breakLengthStart = proposal.getBreakLengthStart();
        breakLengthEnd = proposal.getBreakLengthEnd();
        serviceLengthStart = proposal.getServiceLengthStart();
        serviceLengthEnd = proposal.getServiceLengthEnd();
        alternativeGroup = proposal.getAlternativeGroup();
    }
}
