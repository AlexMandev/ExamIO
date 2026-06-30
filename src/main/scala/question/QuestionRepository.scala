package question

import cats.effect.IO
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

  def getForExam(examId: ExamId): IO[List[Question]] =
    sql"""
         SELECT * FROM questions
         WHERE exam_id = ${examId}
         """.query[Question].to[List].transact(dbTransactor)
