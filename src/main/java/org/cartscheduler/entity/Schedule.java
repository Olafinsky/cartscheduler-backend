package org.cartscheduler.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "schedules")
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "name")
    @NotBlank
    private String name;

    @ManyToMany
    @JoinTable(name = "schedule_access", joinColumns = @JoinColumn(name = "schedule_id"), inverseJoinColumns = @JoinColumn(name = "participant_id"))
    @JsonIgnore
    private List<Participant> accessibleParticipants;

    @OneToMany(mappedBy="schedule")
    private List<ScheduleDay> scheduleDays;
}
