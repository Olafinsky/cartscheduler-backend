package org.cartscheduler.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "schedule_days")
public class ScheduleDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @JsonIgnore
    private long id;

    @ManyToOne
    @JoinColumn(name = "schedule_id")
    @NotNull
    @JsonIgnore
    private Schedule schedule;

    @Column(name = "day_of_week")
    @NotNull
    private short dayOfWeek;

    @Column(name = "schedule_date")
    private LocalDate scheduleDate;

    @Column(name = "name")
    @NotBlank
    private String name;

    @OneToMany(mappedBy = "scheduleDay")
    @OrderBy("hour ASC")
    private List<ScheduleEntry> scheduleEntries;
}
