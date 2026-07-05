ALTER TABLE submissions
ADD deadline TIMESTAMPTZ;

UPDATE submissions s
SET deadline = s.started_at + make_interval(mins => e.time_limit_minutes)
FROM exams e
WHERE s.exam_id = e.id AND s.deadline IS NULL;

ALTER TABLE submissions
ALTER COLUMN deadline SET NOT NULL;
