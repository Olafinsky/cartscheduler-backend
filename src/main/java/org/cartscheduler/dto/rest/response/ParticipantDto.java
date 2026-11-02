package org.cartscheduler.dto.rest.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cartscheduler.entity.Participant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantDto {
    private long id;
    private String name;

    private Boolean hasProposals;

    public ParticipantDto(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public ParticipantDto(Participant participant) {
        id = participant.getId();
        name = participant.getName();
    }

    public ParticipantDto(Participant participant, boolean hasProposals) {
        this(participant);
        this.hasProposals = hasProposals;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Boolean getHasProposals() {
        return hasProposals;
    }
}
