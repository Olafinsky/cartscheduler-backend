-- The former availability-score bands described submitted proposal hours.
-- They now describe ordinal assignments in the generated schedule, so their
-- old values cannot be interpreted safely and are deliberately discarded.
DROP TABLE IF EXISTS `config_candidate_availability_scores`;

CREATE TABLE `config_candidate_assignment_scores` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `assignment_count_from` INTEGER NOT NULL,
    `assignment_count_to` INTEGER NOT NULL,
    `points` INTEGER NOT NULL,
    CONSTRAINT `uq_config_candidate_assignment_score_range`
        UNIQUE (`assignment_count_from`, `assignment_count_to`),
    CONSTRAINT `chk_config_candidate_assignment_score_range`
        CHECK (`assignment_count_from` >= 1 AND `assignment_count_to` >= `assignment_count_from`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `config_pair_assignment_scores` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `assignment_count_from` INTEGER NOT NULL,
    `assignment_count_to` INTEGER NOT NULL,
    `points` INTEGER NOT NULL,
    CONSTRAINT `uq_config_pair_assignment_score_range`
        UNIQUE (`assignment_count_from`, `assignment_count_to`),
    CONSTRAINT `chk_config_pair_assignment_score_range`
        CHECK (`assignment_count_from` >= 1 AND `assignment_count_to` >= `assignment_count_from`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
