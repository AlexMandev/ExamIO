CREATE TABLE IF NOT EXISTS answers (
  question_id UUID NOT NULL REFERENCES questions(id),
  submission_id UUID NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
  data JSONB NOT NULL,
  PRIMARY KEY (question_id, submission_id)
)
