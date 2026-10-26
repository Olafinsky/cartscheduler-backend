package org.cartscheduler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "config_pair_rules")
public class ConfigPairRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_1_id", nullable = false)
    private Participant participant1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_2_id", nullable = false)
    private Participant participant2;

    @Enumerated(EnumType.STRING)
    @Column(name = "compatibility_mode", nullable = false)
    private ConfigPairCompatibilityMode compatibilityMode = ConfigPairCompatibilityMode.ANY;

    @Column(name = "score_points", nullable = false)
    private int scorePoints;
}
