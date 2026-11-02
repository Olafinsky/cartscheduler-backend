CREATE TABLE `enforced_participants` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(128) NOT NULL,
    `participant_1_id` INTEGER NOT NULL,
    `participant_2_id` INTEGER NOT NULL,
    CONSTRAINT `uq_enforced_participants_name` UNIQUE (`name`),
    CONSTRAINT `fk_enforced_participants_participant_1`
        FOREIGN KEY (`participant_1_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_enforced_participants_participant_2`
        FOREIGN KEY (`participant_2_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `config_row_full_or_empty_scores` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `empty_row_points` INTEGER NOT NULL,
    `full_row_adjacent_to_incomplete_points` INTEGER NOT NULL,
    `full_row_not_adjacent_to_incomplete_points` INTEGER NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `config_pair_scores` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `participant_1_id` INTEGER NOT NULL,
    `participant_2_id` INTEGER NOT NULL,
    `points` INTEGER NOT NULL,
    CONSTRAINT `uq_config_pair_scores_participants` UNIQUE (`participant_1_id`, `participant_2_id`),
    CONSTRAINT `fk_config_pair_scores_participant_1`
        FOREIGN KEY (`participant_1_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_config_pair_scores_participant_2`
        FOREIGN KEY (`participant_2_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `config_candidate_uniqueness_scores` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `proposal_appearance_count_from` INTEGER NOT NULL,
    `proposal_appearance_count_to` INTEGER NOT NULL,
    `points` INTEGER NOT NULL,
    CONSTRAINT `uq_config_candidate_uniqueness_score_range`
        UNIQUE (`proposal_appearance_count_from`, `proposal_appearance_count_to`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `config_participant_privilege_scores` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `participant_id` INTEGER NOT NULL,
    `points` INTEGER NOT NULL,
    CONSTRAINT `uq_config_participant_privilege_scores_participant` UNIQUE (`participant_id`),
    CONSTRAINT `fk_config_participant_privilege_scores_participant`
        FOREIGN KEY (`participant_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `config_pair_diversity_scores` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `points` INTEGER NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `config_pair_rules` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `participant_1_id` INTEGER NOT NULL,
    `participant_2_id` INTEGER NOT NULL,
    `rule_type` VARCHAR(16) NOT NULL,
    CONSTRAINT `uq_config_pair_rules_participants` UNIQUE (`participant_1_id`, `participant_2_id`),
    CONSTRAINT `fk_config_pair_rules_participant_1`
        FOREIGN KEY (`participant_1_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_config_pair_rules_participant_2`
        FOREIGN KEY (`participant_2_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `config_participant_break_lengths` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `participant_id` INTEGER NOT NULL,
    `break_length_start` TINYINT NOT NULL,
    `break_length_end` TINYINT NOT NULL,
    CONSTRAINT `uq_config_participant_break_lengths_participant` UNIQUE (`participant_id`),
    CONSTRAINT `fk_config_participant_break_lengths_participant`
        FOREIGN KEY (`participant_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `config_participant_service_lengths` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `participant_id` INTEGER NOT NULL,
    `service_length_start` TINYINT NOT NULL,
    `service_length_end` TINYINT NOT NULL,
    CONSTRAINT `uq_config_participant_service_lengths_participant` UNIQUE (`participant_id`),
    CONSTRAINT `fk_config_participant_service_lengths_participant`
        FOREIGN KEY (`participant_id`) REFERENCES `participants`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
