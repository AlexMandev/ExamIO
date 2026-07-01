package submission

import cats.effect.IO
import cats.data.EitherT
import cats.implicits.*

import io.circe.Codec

import sttp.tapir.Schema

import java.util.UUID

import user.StudentId
import exam.{Exam, ExamId, ExamError, ExamService, ExamCannotBeOpened, ExamStatus}
import cats.instances.map

class SubmissionService(submissionRepository: SubmissionRepository, examService: ExamService):
  def createSubmission(submissionForm: SubmissionForm): IO[Either[ExamError | SubmissionError, Submission]] =
    val result =
      for
        id <- EitherT.liftF(UUID.randomUUID().pure[IO])
        exam <- validateExamStatus(submissionForm.examId, ExamStatus.OPEN)
        _ <- EitherT(checkForAnotherSubmission(submissionForm.examId, submissionForm.studentId))

        newSubmission = NewSubmission(SubmissionId(id), submissionForm.examId, submissionForm.studentId)

        submission <- EitherT(submissionRepository.createSubmission(newSubmission))

      yield submission

    result.value

  def checkForAnotherSubmission(examId: ExamId, studentId: StudentId): IO[Either[AlreadySubmitted, Unit]] =
    for
      maybeSubmission <- submissionRepository.getSubmission(examId, studentId)
      result <- IO(maybeSubmission match
        case None => ().asRight
        case Some(submission) => AlreadySubmitted(studentId, examId, submission.id).asLeft)
    yield result

  // 1. exam exists
  // 2. exam is open
  // 3. submission exists
  // 4. submission has the correct studentId
  // 5. the submission's status is 'InProgress'

  def finishSubmission(studentId: StudentId, submissionId: SubmissionId): IO[Either[ExamError | SubmissionError, Submission]] =
    submissionRepository.finishSubmission(submissionId).map(_.toRight(SubmissionDoesNotExist(submissionId)))

  private def validateExamStatus(examId: ExamId, examStatus: ExamStatus): EitherT[IO, ExamError, Exam] =
    EitherT(examService.findById(examId))
      .ensureOr(exam => ExamCannotBeOpened(exam.id, exam.status))(_.status == ExamStatus.OPEN)

sealed trait SubmissionError derives Codec, Schema
case class AlreadySubmitted(studentId: StudentId, examId: ExamId, submissionId: SubmissionId) extends SubmissionError derives Codec.AsObject, Schema
case class SubmissionDoesNotExist(submissionId: SubmissionId) extends SubmissionError derives Codec.AsObject, Schema

sealed trait SubmissionStatusError extends SubmissionError derives Codec, Schema
case class SubmissionNotInProgress(submissionId: SubmissionId, submissionStatus: SubmissionStatus) extends SubmissionStatusError derives Codec.AsObject, Schema
