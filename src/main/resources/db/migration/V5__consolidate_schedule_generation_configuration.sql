-- The configuration model introduced in V2 was intentionally split by one
-- generator concern per table.  Replace it with cohesive configuration rows.
-- Existing config_* data is deliberately discarded as part of this migration.
-- Schedule, proposal and enforced_participants data is not changed.

DROP TABLE `config_row_full_or_empty_scores`;
DROP TABLE `config_pair_scores`;
DROP TABLE `config_candidate_uniqueness_scores`;
DROP TABLE `config_participant_privilege_scores`;
DROP TABLE `config_pair_diversity_scores`;
DROP TABLE `config_pair_rules`;
DROP TABLE `config_participant_break_lengths`;
DROP TABLE `config_participant_service_lengths`;

CREATE TABLE `config_generation` (
    `id` TINYINT NOT NULL,
    `empty_row_points` INTEGER NOT NULL,
    `full_row_adjacent_to_incomplete_points` INTEGER NOT NULL,
    `full_row_not_adjacent_to_incomplete_points` INTEGER NOT NULL,
    `unique_pair_points` INTEGER NULL,
    CONSTRAINT `pk_config_generation` PRIMARY KEY (`id`),
    CONSTRAINT `chk_config_generation_singleton` CHECK (`id` = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- A neutral default leaves score policy explicitly configurable while keeping
-- console generation usable immediately after the migration.
INSERT INTO `config_generation` (
    `id`,
    `empty_row_points`,
    `full_row_adjacent_to_incomplete_points`,
    `full_row_not_adjacent_to_incomplete_points`,
    `unique_pair_points`
) VALUES (1, 0, 0, 0, NULL);

CREATE TABLE `config_participant_rules` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `participant_id` INTEGER NOT NULL,
    `service_length_min` TINYINT NULL,
    `service_length_max` TINYINT NULL,
    `break_length_min` TINYINT NULL,
    `break_length_max` TINYINT NULL,
    `privilege_points` INTEGER NULL,
    CONSTRAINT `uq_config_participant_rules_participant` UNIQUE (`participant_id`),
    CONSTRAINT `fk_config_participant_rules_participant`
        FOREIGN KEY (`participant_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_config_participant_rules_service_length`
        CHECK (
            (`service_length_min` IS NULL AND `service_length_max` IS NULL)
            OR (`service_length_min` IS NOT NULL AND `service_length_max` IS NOT NULL
                AND `service_length_min` <= `service_length_max`)
        ),
    CONSTRAINT `chk_config_participant_rules_break_length`
        CHECK (
            (`break_length_min` IS NULL AND `break_length_max` IS NULL)
            OR (`break_length_min` IS NOT NULL AND `break_length_max` IS NOT NULL
                AND `break_length_min` <= `break_length_max`)
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `config_pair_rules` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `participant_1_id` INTEGER NOT NULL,
    `participant_2_id` INTEGER NOT NULL,
    `compatibility_mode` VARCHAR(16) NOT NULL DEFAULT 'ANY',
    `score_points` INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT `uq_config_pair_rules_participants` UNIQUE (`participant_1_id`, `participant_2_id`),
    CONSTRAINT `fk_config_pair_rules_participant_1`
        FOREIGN KEY (`participant_1_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_config_pair_rules_participant_2`
        FOREIGN KEY (`participant_2_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_config_pair_rules_canonical_order`
        CHECK (`participant_1_id` < `participant_2_id`),
    CONSTRAINT `chk_config_pair_rules_compatibility_mode`
        CHECK (`compatibility_mode` IN ('ANY', 'REQUIRED', 'FORBIDDEN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `config_candidate_availability_scores` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `appearance_count_from` INTEGER NOT NULL,
    `appearance_count_to` INTEGER NOT NULL,
    `points` INTEGER NOT NULL,
    CONSTRAINT `uq_config_candidate_availability_score_range`
        UNIQUE (`appearance_count_from`, `appearance_count_to`),
    CONSTRAINT `chk_config_candidate_availability_score_range`
        CHECK (`appearance_count_from` >= 0 AND `appearance_count_to` >= `appearance_count_from`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
