CREATE TABLE IF NOT EXISTS submissions (
  id UUID PRIMARY KEY,
  exam_id UUID NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
  student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  finished_at TIMESTAMPTZ,
  status VARCHAR(20) NOT NULL DEFAULT 'InProgress'
    CHECK ((status = 'InProgress' AND finished_at IS NULL)
      OR (status in ('Finished', 'Graded') AND finished_at IS NOT NULL),
  UNIQUE (exam_id, student_id)
)
