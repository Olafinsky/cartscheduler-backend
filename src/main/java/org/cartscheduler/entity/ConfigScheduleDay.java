package org.cartscheduler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Defines the working hours to use for one ISO day of week. */
@Entity
@Getter
@Setter
@Table(name = "config_schedule_days")
public class ConfigScheduleDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    /** ISO-8601 numbering: Monday = 1, Sunday = 7. */
    @Column(name = "day_of_week", nullable = false)
    private short dayOfWeek;

    @Column(name = "hour_from", nullable = false)
    private short hourFrom;

    @Column(name = "hour_to", nullable = false)
    private short hourTo;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
