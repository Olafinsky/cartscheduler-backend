package org.cartscheduler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Optional generation constraints and score adjustment for one participant. */
@Entity
@Getter
@Setter
@Table(name = "config_participant_rules")
public class ConfigParticipantRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(name = "service_length_min")
    private Short serviceLengthMin;

    @Column(name = "service_length_max")
    private Short serviceLengthMax;

    @Column(name = "break_length_min")
    private Short breakLengthMin;

    @Column(name = "break_length_max")
    private Short breakLengthMax;

    @Column(name = "privilege_points")
    private Integer privilegePoints;
}
