package question

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.postgres.circe.jsonb.implicits.*
import infrastructure.db.DBDoobie.DBTransactor
import exam.ExamId

class QuestionRepository(dbTransactor: DBTransactor):
  def nextPosition(examId: ExamId): IO[Int] =
    sql"SELECT COALESCE(MAX(position), 0) + 1 FROM questions WHERE exam_id = $examId"
      .query[Int]
      .unique
      .transact(dbTransactor)

  def addQuestion(question: Question): IO[Question] =
    sql"""INSERT INTO questions (id, exam_id, question_text, question_type, points, position, data)
          VALUES (${question.id}, ${question.examId}, ${question.questionText},
                  ${question.questionType}, ${question.points}, ${question.position}, ${question.data})""".update.run
      .transact(dbTransactor)
      .as(question)
