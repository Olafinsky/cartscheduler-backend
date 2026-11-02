CREATE TABLE `config_schedule_days` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `day_of_week` TINYINT NOT NULL,
    `hour_from` TINYINT NOT NULL,
    `hour_to` TINYINT NOT NULL,
    `is_active` TINYINT NOT NULL DEFAULT 1,
    CONSTRAINT `uq_config_schedule_days_day_of_week` UNIQUE (`day_of_week`),
    CONSTRAINT `chk_config_schedule_days_day_of_week`
        CHECK (`day_of_week` BETWEEN 1 AND 7),
    CONSTRAINT `chk_config_schedule_days_hours`
        CHECK (`hour_from` BETWEEN 0 AND 23 AND `hour_to` BETWEEN `hour_from` AND 23),
    CONSTRAINT `chk_config_schedule_days_is_active`
        CHECK (`is_active` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `config_schedule_restricted_dates` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `restricted_date` DATE NOT NULL,
    CONSTRAINT `uq_config_schedule_restricted_dates_date` UNIQUE (`restricted_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
