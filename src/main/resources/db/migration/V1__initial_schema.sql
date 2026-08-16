CREATE TABLE `participants` (
  `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(256) NOT NULL,
  `email` VARCHAR(256) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `schedules` (
  `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(256) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `schedule_days` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `schedule_id` INTEGER NOT NULL,
    `day_of_week` TINYINT NOT NULL,
    `name` VARCHAR(128) NOT NULL,
    CONSTRAINT `fk_schedule_days_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `schedules`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `schedule_entries` (
  `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `schedule_day_id` INTEGER NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `hour` TINYINT NOT NULL,
  `participant_1_id` INTEGER,
  `participant_2_id` INTEGER,
  CONSTRAINT `fk_schedule_entries_schedule_day` FOREIGN KEY (`schedule_day_id`) REFERENCES `schedule_days`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_schedule_entries_participant_1` FOREIGN KEY (`participant_1_id`) REFERENCES `participants`(`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_schedule_entries_participant_2` FOREIGN KEY (`participant_2_id`) REFERENCES `participants`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `participant_access_tokens` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `participant_id` INTEGER NOT NULL,
    `schedule_id` INTEGER NOT NULL,
    `token` VARCHAR(256) NOT NULL,
    `created_at` DATETIME NOT NULL,
    `expires_at` DATETIME NOT NULL,
    CONSTRAINT `fk_participant_access_tokens_participant` FOREIGN KEY (`participant_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_participant_access_tokens_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `schedules`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `schedule_access` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `schedule_id` INTEGER NOT NULL,
    `participant_id` INTEGER NOT NULL,
    CONSTRAINT `fk_schedule_access_participant` FOREIGN KEY (`participant_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_schedule_access_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `schedules`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `participant_agents` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `agent_participant_id` INTEGER NOT NULL,
    `target_participant_id` INTEGER NOT NULL,
    CONSTRAINT `fk_participant_agents_agent` FOREIGN KEY (`agent_participant_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_participant_agents_target` FOREIGN KEY (`target_participant_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `proposals` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `date_add` DATETIME NOT NULL,
    `participant_id` INTEGER NOT NULL,
    `pair_participant_id` INTEGER,
    `inserting_participant_id` INTEGER,
    `schedule_day_id` INTEGER NOT NULL,
    `hour_start` TINYINT NOT NULL,
    `hour_end` TINYINT NOT NULL,
    `break_length_start` TINYINT,
    `break_length_end` TINYINT,
    `service_length_start` TINYINT,
    `service_length_end` TINYINT,
    CONSTRAINT `fk_proposals_participant` FOREIGN KEY (`participant_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_proposals_pair_participant` FOREIGN KEY (`pair_participant_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_proposals_inserting_participant` FOREIGN KEY (`inserting_participant_id`) REFERENCES `participants`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_proposals_schedule_day` FOREIGN KEY (`schedule_day_id`) REFERENCES `schedule_days`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
