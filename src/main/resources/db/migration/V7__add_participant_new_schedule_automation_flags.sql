ALTER TABLE `participants`
    ADD COLUMN `is_enabled` TINYINT NOT NULL DEFAULT 1 AFTER `email`;

ALTER TABLE `participants`
    ADD COLUMN `new_schedule_autoassignment_enabled` TINYINT NOT NULL DEFAULT 0 AFTER `is_enabled`;

ALTER TABLE `participants`
    ADD COLUMN `new_schedule_autoinvitement_enabled` TINYINT NOT NULL DEFAULT 0
    AFTER `new_schedule_autoassignment_enabled`;
