CREATE INDEX `idx_proposals_schedule_day_participant`
    ON `proposals` (`schedule_day_id`, `participant_id`);
