ALTER TABLE answers
ADD points_awarded NUMERIC(5, 2);

ALTER TABLE submissions
ADD score NUMERIC(8, 2);

ALTER TABLE submissions
ADD CONSTRAINT graded_has_score CHECK (status != 'Graded' OR score IS NOT NULL);