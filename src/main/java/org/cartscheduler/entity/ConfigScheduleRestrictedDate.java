package org.cartscheduler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** A calendar date which must not produce a schedule day. */
@Entity
@Getter
@Setter
@Table(name = "config_schedule_restricted_dates")
public class ConfigScheduleRestrictedDate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "restricted_date", nullable = false)
    private LocalDate restrictedDate;
}
