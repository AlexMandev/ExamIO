CREATE TABLE IF NOT EXISTS exams (
  id UUID PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  time_limit_minutes INT NOT NULL,
  teacher_id UUID NOT NULL REFERENCES users(id),
  status VARCHAR(20) NOT NULL DEFAULT 'draft'
    CHECK (status IN ('draft', 'open', 'closed', 'graded')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
