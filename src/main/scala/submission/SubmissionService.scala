package submission

import cats.effect.IO
import cats.data.EitherT
import cats.implicits.*

import io.circe.Codec

import sttp.tapir.Schema

import java.util.UUID

import user.StudentId
import exam.{ExamId, ExamError, ExamService, ExamStatus, ExamDoesNotExist}

class SubmissionService(submissionRepository: SubmissionRepository, examService: ExamService):
  def getSubmissionById(submissionId: SubmissionId, examId: ExamId, studentId: StudentId)
    : IO[Either[ExamDoesNotExist | SubmissionDoesNotExist | NotSubmissionOwner, Submission]] =
    EitherT(
      submissionRepository
        .getSubmissionById(submissionId)
        .map(_.toRight(SubmissionDoesNotExist(submissionId)))
    )
      .ensureOr(sub => ExamDoesNotExist(examId))(_.examId == examId)
      .ensureOr(sub => NotSubmissionOwner(studentId, sub.id))(_.studentId == studentId)
      .value

  def createSubmission(submissionForm: SubmissionForm): IO[Either[ExamError | SubmissionError, Submission]] =
    val result =
      for
        id <- EitherT.liftF(UUID.randomUUID().pure[IO])
        exam <- examService.fetchAndValidateExamByStatus(
          submissionForm.examId,
          ExamStatus.OPEN,
          "Exam must be open to accept submissions"
        )
        _ <- EitherT(checkForAnotherCreatedSubmission(submissionForm.examId, submissionForm.studentId))

        newSubmission = NewSubmission(SubmissionId(id), submissionForm.examId, submissionForm.studentId)

        submission <- EitherT(submissionRepository.createSubmission(newSubmission))
      yield submission

    result.value

  // 1. exam exists - validateExamStatus
  // 2. exam is open - validateExamStatus
  // 3. submission exists - getSubmission
  // 4. submission has the correct studentId - getSubmission
  // 5. the submission's status is 'InProgress'

  def finishSubmission(studentId: StudentId, submissionId: SubmissionId, examId: ExamId)
    : IO[Either[ExamError | SubmissionError, Submission]] =
    val result =
      for
        // if an exam gets closed before submission,
        // no changes get made
        exam <- examService.fetchAndValidateExamByStatus(
          examId,
          ExamStatus.OPEN,
          "Exam must be open to finish a submission"
        )

        _ <- EitherT(
          submissionRepository
            .getSubmissionById(submissionId)
            .map(_.toRight(SubmissionDoesNotExist(submissionId)))
        )
          .ensureOr(sub => NotSubmissionOwner(studentId, sub.id))(_.studentId == studentId)
          .ensureOr(sub => SubmissionDoesNotExist(submissionId))(_.examId == examId)
          .ensureOr(sub => SubmissionNotInProgress(sub.id, sub.status))(_.status == SubmissionStatus.InProgress)

        submission <- EitherT(
          submissionRepository
            .finishSubmission(submissionId)
            .map(_.toRight(SubmissionDoesNotExist(submissionId)))
        )
      yield submission

    result.value

  private def checkForAnotherCreatedSubmission(examId: ExamId, studentId: StudentId)
    : IO[Either[SubmissionAlreadyExists, Unit]] =
    for
      maybeSubmission <- submissionRepository.getSubmission(examId, studentId)
      result <- IO(maybeSubmission match
        case None => ().asRight
        case Some(submission) => SubmissionAlreadyExists(studentId, examId, submission.id).asLeft)
    yield result

sealed trait SubmissionError derives Codec, Schema
case class SubmissionAlreadyExists(studentId: StudentId, examId: ExamId, submissionId: SubmissionId)
    extends SubmissionError derives Codec.AsObject, Schema
case class AlreadySubmitted(studentId: StudentId, examId: ExamId, submissionId: SubmissionId) extends SubmissionError
    derives Codec.AsObject,
      Schema
case class SubmissionDoesNotExist(submissionId: SubmissionId) extends SubmissionError derives Codec.AsObject, Schema

sealed trait SubmissionPermissionError extends SubmissionError derives Codec, Schema
case class NotSubmissionOwner(studentId: StudentId, submissionId: SubmissionId) extends SubmissionPermissionError
    derives Codec.AsObject,
      Schema

sealed trait SubmissionStatusError extends SubmissionError derives Codec, Schema
case class SubmissionNotInProgress(submissionId: SubmissionId, submissionStatus: SubmissionStatus)
    extends SubmissionStatusError derives Codec.AsObject, Schema
