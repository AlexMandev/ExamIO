package exam

import cats.effect.IO

import doobie.util.*
import doobie.implicits.*
import cats.syntax.all.*
import doobie.postgres.implicits.*

import infrastructure.db.DBDoobie.DBTransactor
import cats.data.NonEmptyChain

class ExamRepository(dbTransactor: DBTransactor):
  def createExam(exam: NewExam): IO[Either[ExamCreationError, Exam]] =
    sql"""
        INSERT INTO exams (id, name, description, time_limit_minutes, teacher_id)
        VALUES (${exam.id}, ${exam.name}, ${exam.description}, ${exam.timeLimitMinutes}, ${exam.teacherId})
        RETURNING *
    """.query[Exam].unique.transact(dbTransactor).map(_.asRight)
