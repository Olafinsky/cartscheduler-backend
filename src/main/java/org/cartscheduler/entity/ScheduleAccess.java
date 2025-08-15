package org.cartscheduler.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "schedule_access")
public class ScheduleAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @ManyToOne
    @JoinColumn(name = "schedule_id")
    @NotNull
    private Schedule schedule;

    @ManyToOne
    @JoinColumn(name = "participant_id")
    @NotNull
    private Participant participant;
}
