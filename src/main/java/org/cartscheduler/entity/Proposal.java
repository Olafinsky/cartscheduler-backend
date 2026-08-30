package org.cartscheduler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Entity
@Getter
@Setter
@Table(name = "proposals")
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "date_add")
    @NotNull
    @CreationTimestamp
    private Date dateAdd;

    @ManyToOne
    @JoinColumn(name = "participant_id")
    @NotNull
    private Participant participant;

    @ManyToOne
    @JoinColumn(name = "pair_participant_id")
    private Participant pairParticipant;

    @ManyToOne
    @JoinColumn(name = "inserting_participant_id")
    private Participant insertingParticipant;

    @ManyToOne
    @JoinColumn(name = "schedule_day_id")
    @NotNull
    private ScheduleDay scheduleDay;

    @Column(name = "hour_start")
    private short hourStart;

    @Column(name = "hour_end")
    private short hourEnd;

    @Column(name = "break_length_start")
    private Short breakLengthStart;

    @Column(name = "break_length_end")
    private Short breakLengthEnd;

    @Column(name = "service_length_start")
    private Short serviceLengthStart;

    @Column(name = "service_length_end")
    private Short serviceLengthEnd;
}
