package org.cartscheduler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Global singleton configuration for schedule generation. */
@Entity
@Getter
@Setter
@Table(name = "config_generation")
public class ConfigGeneration {
    public static final short SINGLETON_ID = 1;

    @Id
    @Column(name = "id")
    private short id;

    @Column(name = "empty_row_points", nullable = false)
    private int emptyRowPoints;

    @Column(name = "full_row_adjacent_to_incomplete_points", nullable = false)
    private int fullRowAdjacentToIncompletePoints;

    @Column(name = "full_row_not_adjacent_to_incomplete_points", nullable = false)
    private int fullRowNotAdjacentToIncompletePoints;

}
