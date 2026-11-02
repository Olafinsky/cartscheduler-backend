ALTER TABLE `participants`
    ADD COLUMN `allow_alternative_group_selection` TINYINT(1) NOT NULL DEFAULT 0
    AFTER `allow_pair_selection`;

ALTER TABLE `proposals`
    ADD COLUMN `alternative_group` VARCHAR(64) NULL
    AFTER `pair_participant_id`;

CREATE INDEX `idx_proposals_participant_alternative_group`
    ON `proposals` (`participant_id`, `alternative_group`);
