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

    @ManyToMany(mappedBy = "accessibleParticipants")
    private List<Schedule> schedules;

    public Participant(Participant participant) {
        id = participant.getId();
        name = participant.getName();
        email = participant.getEmail();
    }

    public Participant() {}
}
