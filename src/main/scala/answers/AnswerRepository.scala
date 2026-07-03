package answer

import cats.effect.IO

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.postgres.circe.jsonb.implicits.*
import doobie.generic.auto.*

import infrastructure.db.DBDoobie.DBTransactor

import question.QuestionId
import submission.SubmissionId

class AnswerRepository(dbTransactor: DBTransactor):
  def getAnswer(questionId: QuestionId, submissionId: SubmissionId): IO[Option[Answer]] =
    sql"""
        SELECT * FROM answers
        WHERE question_id = $questionId AND submission_id = $submissionId
      """
      .query[Answer]
      .option
      .transact(dbTransactor)

  def addAnswer(answer: Answer): IO[Answer] =
    sql"""
        INSERT INTO answers (question_id, submission_id, data)
        VALUES (${answer.questionId}, ${answer.submissionId}, ${answer.data})
      """
      .update
      .run
      .transact(dbTransactor)
      .as(answer)

  def clearAnswer(questionId: QuestionId, submissionId: SubmissionId): IO[Unit] =
    sql"""
        DELETE FROM answers
        WHERE question_id = $questionId AND submission_id = $submissionId
      """
      .update
      .run
      .transact(dbTransactor)
