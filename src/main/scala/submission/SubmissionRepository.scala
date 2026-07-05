package submission

import cats.effect.IO
import cats.implicits.*

import doobie.implicits.*
import doobie.postgres.implicits.*

import infrastructure.db.DBDoobie.DBTransactor

import exam.ExamId
import user.StudentId

class SubmissionRepository(dbTransactor: DBTransactor):
  def createSubmission(newSubmission: NewSubmission, timeLimitInMinutes: Int): IO[Either[SubmissionError, Submission]] =
    sql"""
        INSERT INTO submissions (id, exam_id, student_id, started_at, deadline)
        VALUES (  ${newSubmission.id},
                  ${newSubmission.examId},
                  ${newSubmission.studentId},
                  NOW(),
                  NOW() + make_interval(mins => $timeLimitInMinutes)
               )
        RETURNING *
      """
      .query[Submission]
      .unique
      .transact(dbTransactor)
      .map(_.asRight)

  def autoSubmitPastDeadline: IO[List[Submission]] =
    sql"""
        UPDATE submissions
        SET status = 'Finished',
            finished_at = NOW()
        WHERE status = 'InProgress'
          AND finished_at IS NULL
          AND deadline <= NOW()
        RETURNING *
      """
      .query[Submission]
      .to[List]
      .transact(dbTransactor)

  def getSubmissionById(submissionId: SubmissionId): IO[Option[Submission]] =
    sql"""
        SELECT * FROM submissions
        WHERE id = $submissionId
      """
      .query[Submission]
      .option
      .transact(dbTransactor)

  def getSubmission(examId: ExamId, studentId: StudentId): IO[Option[Submission]] =
    sql"""
        SELECT * FROM submissions
        WHERE exam_id = $examId AND student_id = $studentId
      """
      .query[Submission]
      .option
      .transact(dbTransactor)

  def finishSubmission(submissionId: SubmissionId): IO[Option[Submission]] =
    sql"""
        UPDATE submissions
        SET status = 'Finished', finished_at = NOW()
        WHERE id = $submissionId AND status = 'InProgress' AND finished_at IS NULL
        RETURNING *
      """
      .query[Submission]
      .option
      .transact(dbTransactor)

  def finishAllInProgressForExam(examId: ExamId): IO[Unit] =
    sql"""
        UPDATE submissions
        SET status = 'Finished', finished_at = NOW()
        WHERE exam_id = $examId AND status = 'InProgress'
      """.update.run
      .transact(dbTransactor)
      .void

  def getSubmissionsForExam(examId: ExamId): IO[List[Submission]] =
    sql"""
         SELECT * FROM submissions
         WHERE exam_id = ${examId}
         """.query[Submission].to[List].transact(dbTransactor)

  def gradeSubmission(submissionId: SubmissionId, score: BigDecimal): IO[Unit] =
    sql"""
        UPDATE submissions
        SET status = 'Graded', score = $score
        WHERE id = $submissionId AND status = 'Finished'
      """.update.run
      .transact(dbTransactor)
      .void
