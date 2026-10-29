package org.cartscheduler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Marginal points for ordinal uses of a participant pair in a generated schedule. */
@Entity
@Getter
@Setter
@Table(name = "config_pair_assignment_scores")
public class ConfigPairAssignmentScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "assignment_count_from", nullable = false)
    private int assignmentCountFrom;

    @Column(name = "assignment_count_to", nullable = false)
    private int assignmentCountTo;

    @Column(name = "points", nullable = false)
    private int points;
}
