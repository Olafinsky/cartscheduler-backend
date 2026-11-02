package org.cartscheduler.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "participants")
public class Participant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "name")
    @NotBlank
    private String name;

    @Column(name = "email")
    @NotBlank
    private String email;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "new_schedule_autoassignment_enabled", nullable = false)
    private boolean newScheduleAutoassignmentEnabled;

    @Column(name = "new_schedule_autoinvitement_enabled", nullable = false)
    private boolean newScheduleAutoinvitementEnabled;

    @Column(name = "allow_pair_selection", nullable = false)
    private boolean allowPairSelection;

    @Column(name = "allow_alternative_group_selection", nullable = false)
    private boolean allowAlternativeGroupSelection;

    @ManyToMany(mappedBy = "accessibleParticipants")
    private List<Schedule> schedules;

    @Transient
    private ParticipantAccessToken currentAccessToken;

    @ManyToMany
    @JoinTable(
            name = "participant_agents",
            joinColumns = @JoinColumn(name = "agent_participant_id"),
            inverseJoinColumns = @JoinColumn(name = "target_participant_id")
    )
    private List<Participant> assignedParticipants;

    @ManyToMany(mappedBy = "assignedParticipants")
    private List<Participant> agentParticipants;

    public Participant(Participant participant) {
        id = participant.getId();
        name = participant.getName();
        email = participant.getEmail();
        enabled = participant.isEnabled();
        newScheduleAutoassignmentEnabled = participant.isNewScheduleAutoassignmentEnabled();
        newScheduleAutoinvitementEnabled = participant.isNewScheduleAutoinvitementEnabled();
        allowPairSelection = participant.isAllowPairSelection();
        allowAlternativeGroupSelection = participant.isAllowAlternativeGroupSelection();
    }

    public Participant() {}
}
