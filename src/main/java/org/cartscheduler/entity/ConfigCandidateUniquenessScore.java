package org.cartscheduler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "config_candidate_uniqueness_scores")
public class ConfigCandidateUniquenessScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "proposal_appearance_count_from")
    private int proposalAppearanceCountFrom;

    @Column(name = "proposal_appearance_count_to")
    private int proposalAppearanceCountTo;

    @Column(name = "points")
    private int points;
}
