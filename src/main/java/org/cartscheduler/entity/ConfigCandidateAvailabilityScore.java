package org.cartscheduler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Points per occupied cell for a candidate with a matching availability count. */
@Entity
@Getter
@Setter
@Table(name = "config_candidate_availability_scores")
public class ConfigCandidateAvailabilityScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "appearance_count_from", nullable = false)
    private int appearanceCountFrom;

    @Column(name = "appearance_count_to", nullable = false)
    private int appearanceCountTo;

    @Column(name = "points", nullable = false)
    private int points;
}
