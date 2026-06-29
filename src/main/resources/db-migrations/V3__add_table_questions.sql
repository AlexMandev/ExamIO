CREATE TABLE IF NOT EXISTS questions (
  id UUID PRIMARY KEY,
  exam_id UUID NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
  question_text TEXT NOT NULL,
  question_type VARCHAR(20) NOT NULL CHECK (question_type IN ('MultipleChoice', 'TrueFalse', 'ShortAnswer')),
  points NUMERIC(5, 2) NOT NULL CHECK (points > 0),
  position INT NOT NULL,
  data JSONB NOT NULL,
  UNIQUE (exam_id, position)
);
