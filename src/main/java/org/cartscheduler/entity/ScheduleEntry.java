package org.cartscheduler.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "schedule_entries")
public class ScheduleEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @JsonIgnore
    private long id;

    @Column(name = "name")
    @NotBlank
    private String name;

    @ManyToOne
    @JoinColumn(name = "schedule_day_id")
    @NotNull
    @JsonIgnore
    private ScheduleDay scheduleDay;

    @Column(name = "hour")
    @NotNull
    private short hour;

    @ManyToOne
    @JoinColumn(name = "participant_1_id")
    @JsonIgnore
    private Participant participant1;

    @ManyToOne
    @JoinColumn(name = "participant_2_id")
    @JsonIgnore
    private Participant participant2;
}
