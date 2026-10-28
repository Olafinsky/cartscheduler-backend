CREATE TABLE `users` (
    `id` INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(128) NOT NULL,
    `name` VARCHAR(128) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `email` VARCHAR(128) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `date_last_login` DATETIME NULL,
    CONSTRAINT `uq_users_username` UNIQUE (`username`),
    CONSTRAINT `uq_users_email` UNIQUE (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Default credentials: admin / Admin123. The stored value is a BCrypt hash.
-- ON DUPLICATE KEY keeps an administrator's changed password intact.
INSERT INTO `users` (`username`, `name`, `password`, `email`, `created_at`)
VALUES (
    'admin',
    'Administrator',
    '$2a$10$CZivbAcVzD8VFbsvi8FXhOfpFUH/5khd.n9FQxgcDtxDoR658egPO',
    'admin@localhost.local',
    CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE `id` = `id`;
