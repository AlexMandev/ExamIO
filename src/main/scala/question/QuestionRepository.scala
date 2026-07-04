package question

import cats.effect.IO
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.postgres.circe.jsonb.implicits.*
import doobie.generic.auto.*
import io.circe.syntax.*
import infrastructure.db.DBDoobie.DBTransactor
import exam.ExamId

class QuestionRepository(dbTransactor: DBTransactor):
  // COEALESCE returns the first argument that is not NULL
  def nextPosition(examId: ExamId): IO[Int] =
    sql"SELECT COALESCE(MAX(position), 0) + 1 FROM questions WHERE exam_id = $examId"
      .query[Int]
      .unique
      .transact(dbTransactor)

  def addQuestion(q: Question): IO[Question] =
    sql"""INSERT INTO questions (id, exam_id, question_text, question_type, points, position, data)
          VALUES (${q.id}, ${q.examId}, ${q.questionText},
                  ${q.questionType}, ${q.points}, ${q.position}, ${q.data.asJson})""".update.run
      .transact(dbTransactor)
      .as(q)

  def getQuestionById(questionId: QuestionId, examId: ExamId): IO[Option[Question]] =
    sql"""
        SELECT * FROM questions
        WHERE id = $questionId AND exam_id = $examId
      """
      .query[Question]
      .option
      .transact(dbTransactor)

  def getQuestionsByExamId(examId: ExamId): IO[List[Question]] =
    sql"""
         SELECT * FROM questions
         WHERE exam_id = ${examId}
         """.query[Question].to[List].transact(dbTransactor)

  def deleteQuestion(questionId: QuestionId, examId: ExamId): IO[Boolean] =
    val deleteQuery: ConnectionIO[Boolean] =
      for
        maybePos <- getQuestionPosition(examId, questionId)
        result <- maybePos match
          case None => false.pure[ConnectionIO]
          case Some(pos) =>
            deleteQuestionById(questionId) >>
              decrementQuestionPositions(examId, pos) >> true.pure[ConnectionIO]
      yield result

    deleteQuery.transact(dbTransactor)

  private def getQuestionPosition(examId: ExamId, questionId: QuestionId): ConnectionIO[Option[Int]] =
    sql"""
        SELECT position FROM questions
        WHERE id = $questionId AND exam_id = $examId
      """
      .query[Int]
      .option

  private def deleteQuestionById(questionId: QuestionId): ConnectionIO[Int] =
    sql"DELETE FROM questions WHERE id = $questionId".update.run

  private def decrementQuestionPositions(examId: ExamId, position: Int): ConnectionIO[Int] =
    sql"""
        UPDATE questions SET position = position - 1
        WHERE exam_id = $examId AND position > $position"
      """.update.run
