CREATE TABLE IF NOT EXISTS `participants` (
  `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(256) NOT NULL,
  `email` VARCHAR(256)
);


CREATE TABLE IF NOT EXISTS `schedules` (
  `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(256) NOT NULL
);

CREATE TABLE IF NOT EXISTS `schedule_days` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `schedule_id` INTEGER NOT NULL,
    `day_of_week` TINYINT NOT NULL,
    `name` VARCHAR(128) NOT NULL,
    FOREIGN KEY `fk_schedule_id`(`schedule_id`) REFERENCES `schedules`(`id`) ON DELETE CASCADE
)

CREATE TABLE IF NOT EXISTS `schedule_entries` (
  `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `schedule_day_id` INTEGER NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `hour` TINYINT NOT NULL,
  `participant_1_id` INTEGER,
  `participant_2_id` INTEGER,
  FOREIGN KEY `fk_schedule_day_id`(`schedule_day_id`) REFERENCES `schedule_days`(`id`) ON DELETE CASCADE,
  FOREIGN KEY `fk_participant_1_id`(`participant_1_id`) REFERENCES `participants`(`id`) ON DELETE SET NULL,
  FOREIGN KEY `fk_participant_2_id`(`participant_2_id`) REFERENCES `participants`(`id`) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS `participant_access_tokens` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `participant_id` INTEGER NOT NULL,
    `schedule_id` INTEGER NOT NULL,
    `token` VARCHAR(256) NOT NULL,
    `created_at` DATETIME NOT NULL,
    `expires_at` DATETIME NOT NULL,
    FOREIGN KEY (`participant_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`schedule_id`) REFERENCES `schedules`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `schedule_access` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `schedule_id` INTEGER NOT NULL,
    `participant_id` INTEGER NOT NULL,
     FOREIGN KEY `fk_participant_id`(`participant_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE,
     FOREIGN KEY `fk_schedule_id`(`schedule_id`) REFERENCES `schedules`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `participant_agents` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `agent_participant_id` INTEGER NOT NULL,
    `target_participant_id` INTEGER NOT NULL,
    FOREIGN KEY `fk_agent_participant_id`(`agent_participant_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE,
    FOREIGN KEY `fk_target_participant_id`(`target_participant_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE
);

!-- TODO
CREATE TABLE IF NOT EXISTS `proposals` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `schedule_entry_id` INTEGER NOT NULL
)
