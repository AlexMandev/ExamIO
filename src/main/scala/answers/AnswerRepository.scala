package answer

import cats.effect.IO

import io.circe.syntax.*

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

  // create if it doesn't exist, replace it otherwise
  def addAnswer(answer: Answer): IO[Answer] =
    sql"""
      INSERT INTO answers (question_id, submission_id, data)
      VALUES (${answer.questionId}, ${answer.submissionId}, ${answer.data.asJson})
      ON CONFLICT (question_id, submission_id)
      DO UPDATE SET data = EXCLUDED.data
    """.update.run
      .transact(dbTransactor)
      .as(answer)

  def clearAnswer(questionId: QuestionId, submissionId: SubmissionId): IO[Unit] =
    sql"""
        DELETE FROM answers
        WHERE question_id = $questionId AND submission_id = $submissionId
      """.update.run
      .transact(dbTransactor)
      .void

  def getAllForSubmissionOrderedByQuestionPosition(submissionId: SubmissionId): IO[List[Answer]] =
    sql"""
         SELECT * FROM answers a
         JOIN questions q ON a.question_id = q.id
         WHERE submission_id = ${submissionId}
         ORDER BY q.position ASC
         """.query[Answer].to[List].transact(dbTransactor)

  def setPointsAwarded(questionId: QuestionId, submissionId: SubmissionId, points: BigDecimal): IO[Unit] =
    sql"""
        UPDATE answers
        SET points_awarded = $points
        WHERE question_id = $questionId AND submission_id = $submissionId
      """.update.run
      .transact(dbTransactor)
      .void
