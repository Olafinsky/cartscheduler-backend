package org.cartscheduler.dto.rest.response;

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

    public ParticipantDto(Participant participant) {
        id = participant.getId();
        name = participant.getName();
    }
}
