-- config_generation is a singleton managed only through editing in the admin panel.
-- Normalize legacy nullable values and ensure a neutral row always exists.
UPDATE `config_generation`
SET `unique_pair_points` = 0
WHERE `unique_pair_points` IS NULL;

ALTER TABLE `config_generation`
    MODIFY COLUMN `empty_row_points` INTEGER NOT NULL DEFAULT 0,
    MODIFY COLUMN `full_row_adjacent_to_incomplete_points` INTEGER NOT NULL DEFAULT 0,
    MODIFY COLUMN `full_row_not_adjacent_to_incomplete_points` INTEGER NOT NULL DEFAULT 0,
    MODIFY COLUMN `unique_pair_points` INTEGER NOT NULL DEFAULT 0;

INSERT INTO `config_generation` (
    `id`,
    `empty_row_points`,
    `full_row_adjacent_to_incomplete_points`,
    `full_row_not_adjacent_to_incomplete_points`,
    `unique_pair_points`
) VALUES (1, 0, 0, 0, 0)
ON DUPLICATE KEY UPDATE `id` = `id`;
