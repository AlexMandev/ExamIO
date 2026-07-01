package exam

import cats.effect.IO

import doobie.util.*
import doobie.implicits.*
import cats.syntax.all.*
import doobie.postgres.implicits.*

import infrastructure.db.DBDoobie.DBTransactor

class ExamRepository(dbTransactor: DBTransactor):
  def createExam(exam: NewExam): IO[Either[ExamCreationError, Exam]] =
    sql"""
        INSERT INTO exams (id, name, description, time_limit_minutes, teacher_id)
        VALUES (${exam.id}, ${exam.name}, ${exam.description}, ${exam.timeLimitMinutes}, ${exam.teacherId})
        RETURNING *
    """.query[Exam].unique.transact(dbTransactor).map(_.asRight)

  def getExamsBy(teacherId: TeacherId): IO[List[Exam]] =
    sql"""
        SELECT * FROM exams WHERE teacher_id = ${teacherId}
      """
      .query[Exam]
      .to[List]
      .transact(dbTransactor)

  def getExamById(examId: ExamId): IO[Option[Exam]] =
    sql"""
        SELECT * FROM exams WHERE id = ${examId}
      """
      .query[Exam]
      .option
      .transact(dbTransactor)

  def openExamById(examId: ExamId): IO[Unit] =
    sql"""
        UPDATE exams
        SET status = 'open'
        WHERE id = ${examId} AND status = 'draft'
      """.update.run
      .transact(dbTransactor)
      .void

  def closeExamById(examId: ExamId): IO[Unit] =
    sql"""
        UPDATE exams
        SET status = 'closed'
        WHERE id = ${examId} AND status = 'open'
      """.update.run
      .transact(dbTransactor)
      .void
