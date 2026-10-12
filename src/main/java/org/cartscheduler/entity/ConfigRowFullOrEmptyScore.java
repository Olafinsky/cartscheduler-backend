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
@Table(name = "config_row_full_or_empty_scores")
public class ConfigRowFullOrEmptyScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "empty_row_points")
    private int emptyRowPoints;

    @Column(name = "full_row_adjacent_to_incomplete_points")
    private int fullRowAdjacentToIncompletePoints;

    @Column(name = "full_row_not_adjacent_to_incomplete_points")
    private int fullRowNotAdjacentToIncompletePoints;
}
