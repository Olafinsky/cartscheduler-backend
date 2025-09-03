package org.cartscheduler.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "participant_agents")
public class ParticipantAgent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @ManyToOne
    @JoinColumn(name = "agent_participant_id")
    @NotNull
    private Participant agentParticipant;

    @ManyToOne
    @JoinColumn(name = "target_participant_id")
    @NotNull
    private Participant targetParticipant;
}
