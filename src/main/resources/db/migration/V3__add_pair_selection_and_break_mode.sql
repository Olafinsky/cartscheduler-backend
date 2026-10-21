ALTER TABLE `participants`
    ADD COLUMN `allow_pair_selection` TINYINT(1) NOT NULL DEFAULT 0 AFTER `email`;

ALTER TABLE `proposals`
    ADD COLUMN `break_mode` TINYINT NOT NULL DEFAULT 2 AFTER `hour_end`;

-- Preserve the semantics of proposals created before break_mode was introduced:
-- an entered break-length range meant that a break was requested.
UPDATE `proposals`
SET `break_mode` = CASE
    WHEN `break_length_start` IS NULL AND `break_length_end` IS NULL THEN 0
    ELSE 1
END;

ALTER TABLE `proposals`
    ADD CONSTRAINT `chk_proposals_break_mode`
    CHECK (`break_mode` IN (0, 1, 2));
