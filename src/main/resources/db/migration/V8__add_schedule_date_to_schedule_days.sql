ALTER TABLE `schedule_days`
    ADD COLUMN `schedule_date` DATE NULL AFTER `day_of_week`;

-- Calendar schedules created before this migration already store an ISO date
-- at the beginning of their display name. Persist it once so the API can read
-- dates from the data model rather than deriving them on every request.
UPDATE `schedule_days`
SET `schedule_date` = STR_TO_DATE(SUBSTRING(`name`, 1, 10), '%Y-%m-%d')
WHERE `name` REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}';
